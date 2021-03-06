package com.amity.sample.ascsdk.messagelist

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItems
import com.amity.sample.ascsdk.R
import com.amity.sample.ascsdk.common.extensions.showDialog
import com.amity.sample.ascsdk.common.extensions.showToast
import com.amity.sample.ascsdk.common.file.FileManager
import com.amity.sample.ascsdk.common.preferences.SamplePreferences
import com.amity.sample.ascsdk.intent.OpenMessageReactionListIntent
import com.amity.sample.ascsdk.intent.ViewChannelMembershipsIntent
import com.amity.sample.ascsdk.messagelist.option.MessageOption
import com.amity.sample.ascsdk.messagelist.option.ReactionOption
import com.amity.socialcloud.sdk.AmityCoreClient
import com.amity.socialcloud.sdk.chat.AmityChatClient
import com.amity.socialcloud.sdk.chat.channel.AmityChannel
import com.amity.socialcloud.sdk.chat.message.AmityMessage
import com.amity.socialcloud.sdk.core.AmityTags
import com.amity.socialcloud.sdk.core.error.AmityError
import com.amity.socialcloud.sdk.core.permission.AmityPermission
import com.amity.socialcloud.sdk.core.user.AmityUser
import com.ekoapp.core.utils.getCurrentClassAndMethodNames
import com.ekoapp.ekosdk.internal.AmityPagingDataRefresher
import com.google.common.base.Joiner
import com.google.common.collect.Sets
import com.google.gson.JsonObject
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_message_list.*
import timber.log.Timber

abstract class MessageListActivity : AppCompatActivity() {

    private var messages: LiveData<PagedList<AmityMessage>>? = null

    private var adapter: MessageListAdapter? = null

    protected val channelRepository = AmityChatClient.newChannelRepository()
    protected val messageRepository = AmityChatClient.newMessageRepository()
    protected val userRepository = AmityCoreClient.newUserRepository()

    protected val includingTags: MutableSet<String> = Sets.newConcurrentHashSet()
    protected val excludingTags: MutableSet<String> = Sets.newConcurrentHashSet()

    protected val stackFromEnd = SamplePreferences.getStackFromEnd(javaClass.name, getDefaultStackFromEnd())
    protected val revertLayout = SamplePreferences.getRevertLayout(javaClass.name, getDefaultRevertLayout())

    protected val rxPermissions = RxPermissions(this)

    protected val disposable = CompositeDisposable()
    protected var checkPermissionDisposable = CompositeDisposable()

    protected abstract fun getChannelId(): String

    protected abstract fun getMenu(): Int

    protected abstract fun getMessageCollection(): LiveData<PagedList<AmityMessage>>

    protected abstract fun getDefaultStackFromEnd(): Boolean

    protected abstract fun getDefaultRevertLayout(): Boolean

    protected abstract fun setTitleName()

    protected abstract fun setSubtitleName()

    protected abstract fun startReading()

    protected abstract fun stopReading()

    protected abstract fun onClick(message: AmityMessage)

    protected abstract fun createTextMessage(text: String): Completable


    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setContentView(R.layout.activity_message_list)

        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        setSupportActionBar(toolbar)

        setTitleName()
        setSubtitleName()
        setUpInputLayout()

        initialMessageCollection()

    }

    override fun onStart() {
        super.onStart()
        observeMessageCollection()
        startReading()
    }

    override fun onStop() {
        super.onStop()
        stopReading()
    }

    override fun onPause() {
        super.onPause()
        checkPermissionDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
        checkPermissionDisposable.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(getMenu(), menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_channel_membership) {
            startActivity(ViewChannelMembershipsIntent(this, getChannelId()))
            return true
        } else if (item.itemId == R.id.action_leave_channel) {
            channelRepository.leaveChannel(getChannelId())
                    .doOnComplete(Action { this.finish() })
                    .subscribe()
            return true
        } else if (item.itemId == R.id.action_edit_channel) {
            showDialog(R.string.edit_channel, "displayName", "", false) { d, input ->
                channelRepository.updateChannel(getChannelId())
                        .displayName(input.toString())
                        .build()
                        .update()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            finish()
                        }, {
                            showToast(message = "Update failed !")
                        })
            }
        } else if (item.itemId == R.id.action_with_tags) {
            showDialog(R.string.with_tag, "bnk48,football,concert", Joiner.on(",").join(includingTags), true, { dialog, input ->
                includingTags.clear()
                for (tag in input.toString().split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                    if (tag.length > 0) {
                        includingTags.add(tag)
                    }
                }
                initialMessageCollection()
                observeMessageCollection()
            })
            return true
        } else if (item.itemId == R.id.action_without_tags) {
            showDialog(R.string.with_tag, "bnk48,football,concert", Joiner.on(",").join(excludingTags), true, { dialog, input ->
                excludingTags.clear()
                for (tag in input.toString().split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                    if (tag.length > 0) {
                        excludingTags.add(tag)
                    }
                }
                initialMessageCollection()
                observeMessageCollection()
            })
            return true
        } else if (item.itemId == R.id.action_set_tags) {
            val liveData = LiveDataReactiveStreams.fromPublisher(channelRepository.getChannel(getChannelId()))
            liveData.observeForever(object : Observer<AmityChannel> {
                override fun onChanged(channel: AmityChannel) {
                    liveData.removeObserver(this)
                    showDialog(R.string.set_tags, "bnk48,football,concert", Joiner.on(",").join(channel.getTags()), true, { dialog, input ->
                        val set = Sets.newConcurrentHashSet<String>()
                        for (tag in input.toString().split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                            if (tag.length > 0) {
                                set.add(tag)
                            }
                        }
                        channelRepository.updateChannel(channel.getChannelId())
                                .tags(AmityTags(set))
                                .build()
                                .update()
                                .subscribeOn(Schedulers.io())
                                .subscribe()
                    })
                }
            })
            return true
        } else if (item.itemId == R.id.action_notification_for_current_channel) {
            channelRepository.notification(getChannelId())
                    .getSettings()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { settings ->
                        MaterialDialog(this).show {
                            checkBoxPrompt(text = "allow notification for current channel", isCheckedDefault = settings.isEnabled(), onToggle = null)
                            positiveButton(text = "save changes") {
                                if (isCheckPromptChecked()) {
                                    channelRepository.notification(getChannelId())
                                            .enable()
                                            .subscribeOn(Schedulers.io())
                                            .subscribe()
                                } else {
                                    channelRepository.notification(getChannelId())
                                            .disable()
                                            .subscribeOn(Schedulers.io())
                                            .subscribe()
                                }
                            }
                            negativeButton(text = "discard")

                        }
                    }
                    .subscribe()
            return true
        } else if (item.itemId == R.id.action_stack_from_end) {
            MaterialDialog(this).show {
                checkBoxPrompt(text = getString(R.string.stack_from_end), isCheckedDefault = stackFromEnd.get(), onToggle = null)
                positiveButton(text = "save change") {
                    stackFromEnd.set(isCheckPromptChecked())
                    initialMessageCollection()
                    observeMessageCollection()
                }
                negativeButton(text = "discard")
            }

            return true
        } else if (item.itemId == R.id.action_revert_layout) {

            MaterialDialog(this).show {
                checkBoxPrompt(text = getString(R.string.revert_layout), isCheckedDefault = revertLayout.get(), onToggle = null)
                positiveButton(text = "save change") {
                    revertLayout.set(isCheckPromptChecked())
                    initialMessageCollection()
                    observeMessageCollection()
                }
                negativeButton(text = "discard")
            }
            return true
        } else if (item.itemId == R.id.action_check_permission) {
            showDialog(R.string.check_permission, "", AmityPermission.EDIT_USER.value, false) { dialog, input ->
                val inputStr = input.toString()
                if (AmityPermission.values().any { it.value == inputStr }) {
                    checkPermissionDisposable.add(
                            AmityCoreClient.hasPermission(AmityPermission.valueOf(inputStr))
                                    .atChannel(getChannelId())
                                    .check()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnNext {
                                        showToast("You have permission or not?: $it")
                                    }
                                    .doOnError { Timber.d("${getCurrentClassAndMethodNames()}${it.message}") }
                                    .subscribe())
                } else {
                    showToast("Your permission is invalid")
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpInputLayout() {
        setUpSendButton()

        LiveDataReactiveStreams.fromPublisher(AmityChatClient.newChannelRepository().getChannel(getChannelId())).observe(this, Observer {
            if (it.getType() == AmityChannel.Type.BROADCAST) {
                message_input_layout.visibility = View.GONE
            }
        })
    }

    private fun onLongClick(message: AmityMessage) {

        if (message.isDeleted()) {
            return
        }

        val actionItems = getMessageOptions(message)
        MaterialDialog(this).show {
            listItems(items = actionItems) { dialog, position, text ->
                when (MessageOption.enumOf(text.toString())) {
                    MessageOption.FLAG_MESSAGE -> {
                        flagMessage(message)
                    }
                    MessageOption.FLAG_SENDER -> {
                        flagUser(message.getUser())
                    }
                    MessageOption.SET_TAG -> {
                        setTags(message)
                    }
                    MessageOption.ADD_REACTION -> {
                        showAddReactionDialog(message)
                    }
                    MessageOption.REMOVE_REACTION -> {
                        showRemoveReactionDialog(message)
                    }
                    MessageOption.EDIT -> {
                        editMessage(message)
                    }
                    MessageOption.DELETE -> {
                        deleteMessage(message)
                    }
                    MessageOption.OPEN_FILE -> {
                        openFile(message.getData() as AmityMessage.Data.FILE)
                    }
                    MessageOption.REACTION_HISTORY -> {
                        showReactionHistory(message)
                    }
                }
            }
        }
    }

    private fun getMessageOptions(message: AmityMessage): List<String> {
        val optionItems = mutableListOf<String>()
        optionItems.add(MessageOption.FLAG_MESSAGE.value)
        optionItems.add(MessageOption.FLAG_SENDER.value)
        optionItems.add(MessageOption.SET_TAG.value)

        if (message.getDataType() == AmityMessage.DataType.FILE) {
            optionItems.add(MessageOption.OPEN_FILE.value)
        }

        if (message.getUserId() == AmityCoreClient.getUserId()) {
            if (message.getDataType() == AmityMessage.DataType.TEXT || message.getDataType() == AmityMessage.DataType.CUSTOM) {
                optionItems.add(MessageOption.EDIT.value)
            }
            optionItems.add(MessageOption.DELETE.value)
        }

        optionItems.add(MessageOption.ADD_REACTION.value)
        if (message.getMyReactions().isNotEmpty()) {
            optionItems.add(MessageOption.REMOVE_REACTION.value)
        }
        optionItems.add(MessageOption.REACTION_HISTORY.value)

        return optionItems
    }

    private fun editMessage(message: AmityMessage) {
        val data = message.getData()
        when (data) {
            is AmityMessage.Data.TEXT -> {
                showTextMessageEditor(data)
            }

            is AmityMessage.Data.CUSTOM -> {
                showCustomMessageEditor(data)
            }
        }
    }

    private fun deleteMessage(message: AmityMessage) {
        message.delete()
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    private fun flagMessage(message: AmityMessage) {
        if (message.isFlaggedByMe()) {
            MaterialDialog(this).show {
                title = "un-flag a message"
                positiveButton(text = "un-flag a message") {
                    disposable.add(
                            message.report()
                                    .unflag()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete {
                                        Toast.makeText(this@MessageListActivity, "successfully un-flagged a message", Toast.LENGTH_SHORT).show()
                                    }
                                    .subscribe())
                }
            }

        } else {
            MaterialDialog(this).show {
                title = "flag a message"
                positiveButton(text = "flag") {
                    disposable.add(
                            message.report()
                                    .flag()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete {
                                        Toast.makeText(this@MessageListActivity, "successfully flagged the a message", Toast.LENGTH_SHORT).show()
                                    }
                                    .subscribe())
                }
            }
        }
    }

    private fun flagUser(user: AmityUser?) {
        if (user?.isFlaggedByMe() ?: false) {
            MaterialDialog(this).show {
                title = "un-flag a sender"
                positiveButton(text = "un-flag") {
                    disposable.add(userRepository.report(user!!.getUserId())
                            .unflag()
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete { Toast.makeText(this@MessageListActivity, "successfully un-flagged a sender", Toast.LENGTH_SHORT).show() }
                            .subscribe())
                }
            }

        } else {
            MaterialDialog(this).show {
                title = "flag a sender"
                positiveButton(text = "flag") {
                    disposable.add(userRepository.report(user!!.getUserId())
                            .flag()
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete { Toast.makeText(this@MessageListActivity, "successfully flagged a sender", Toast.LENGTH_SHORT).show() }
                            .subscribe())
                }
            }
        }
    }

    private fun setTags(message: AmityMessage) {
        showDialog(R.string.set_tags, "bnk48,football,concert", Joiner.on(",").join(message.getTags()), true) { dialog, input ->
            val set = Sets.newConcurrentHashSet<String>()
            for (tag in input.toString().split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                if (tag.isNotEmpty()) {
                    set.add(tag)
                }
            }

            val data = message.getData()
            var update = Completable.complete()
            when(data) {
                is AmityMessage.Data.TEXT -> {
                    update = data.edit().tags(AmityTags(set))
                            .build()
                            .apply()
                }
                is AmityMessage.Data.CUSTOM -> {
                    update = data.edit().tags(AmityTags(set))
                            .build()
                            .apply()
                }
            }
            update.subscribeOn(Schedulers.io())
                    .subscribe()
        }
    }

    private fun initialMessageCollection() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = stackFromEnd.get()
        layoutManager.reverseLayout = revertLayout.get()
        message_list_recyclerview.layoutManager = layoutManager

        adapter = MessageListAdapter()
        message_list_recyclerview.adapter = adapter
        message_list_recyclerview.addOnScrollListener(AmityPagingDataRefresher(stackFromEnd.get()))

        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val lastPosition = adapter!!.itemCount - 1
                if (positionStart == lastPosition) {
                    message_list_recyclerview.scrollToPosition(lastPosition)
                }
            }
        })

        disposable.clear()

        disposable.add(adapter!!.onLongClickFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    onLongClick(it!!)
                }
                .subscribe())

        disposable.add(adapter!!.onClickFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    onClick(it!!)
                }
                .subscribe())
    }

    private fun observeMessageCollection() {
        if (messages != null) {
            messages?.removeObservers(this)
        }

        messages = getMessageCollection()
        messages?.observe(this, Observer {
            adapter?.submitList(it)
        })
    }

    private fun setUpSendButton() {
        message_edittext.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                message_send_button.isEnabled = !TextUtils.isEmpty(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }
        })

        message_send_button.setOnClickListener {
            val text = message_edittext.text.toString().trim()
            message_edittext.text = null
            createTextMessage(text)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { t ->
                        val amityError = AmityError.from(t)
                        if (amityError == AmityError.USER_IS_BANNED) {
                            message_edittext.post {
                                Toast.makeText(this, t.message, Toast.LENGTH_SHORT).show()
                            }
                            message_edittext.postDelayed({ finish() }, 500)
                        }
                    }
                    .subscribe()

        }
    }

    private fun openFile(data: AmityMessage.Data.FILE) {
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        FileManager.openFile(getApplicationContext(), data)
                    }
                }
    }

    private fun showTextMessageEditor(data: AmityMessage.Data.TEXT) {
        showDialog(R.string.edit_text_message, "enter text", data.getText(), false) { dialog, input ->
            val modifiedText = input.toString()
            if (modifiedText != data.getText()) {
                data.edit()
                        .text(modifiedText)
                        .build()
                        .apply()
                        .subscribe()
            }
        }
    }

    private fun showCustomMessageEditor(data: AmityMessage.Data.CUSTOM) {
        val dialog = MaterialDialog(this)
                .title(text = "Edit custom message")
                .customView(R.layout.view_custom_message_editor, scrollable = true)

        val customView = dialog.getCustomView()
        val keyEditText = customView.findViewById<EditText>(R.id.key_edittext)
        val valueEditText = customView.findViewById<EditText>(R.id.value_edittext)
        val sendButton = customView.findViewById<Button>(R.id.send_button)

        keyEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendButton.isEnabled = s?.isNotEmpty() ?: false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }
        })

        sendButton.setOnClickListener {
            it.isEnabled = false
            sendEditCustomMessageRequest(data, keyEditText.text.toString().trim(), valueEditText.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sendEditCustomMessageRequest(data: AmityMessage.Data.CUSTOM, key: String, value: String) {
        val customData = JsonObject()
        customData.addProperty(key, value)

        data.edit()
                .data(customData)
                .build()
                .apply()
                .subscribe()

    }

    private fun showAddReactionDialog(message: AmityMessage) {
        val reactionItems = mutableListOf<String>()
        ReactionOption.values().filter {
            !message.getMyReactions().contains(it.value())
        }.forEach {
            reactionItems.add(it.value())
        }

        if (reactionItems.isNullOrEmpty()) {
            return
        }

        MaterialDialog(this).show {
            listItems(items = reactionItems) { dialog, position, text ->
                message.react()
                        .addReaction(text.toString())
                        .subscribe()
            }
        }
    }

    private fun showRemoveReactionDialog(message: AmityMessage) {
        val reactionItems = message.getMyReactions()
        MaterialDialog(this).show {
            listItems(items = reactionItems) { dialog, position, text ->
                message.react()
                        .removeReaction(text.toString())
                        .subscribe()
            }
        }
    }

    private fun showReactionHistory(message: AmityMessage) {
        startActivity(OpenMessageReactionListIntent(this, message))
    }

}