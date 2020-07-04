package com.ekoapp.sample.chatfeature.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.ekoapp.ekosdk.EkoMessage
import com.ekoapp.ekosdk.messaging.data.ImageData
import com.ekoapp.sample.chatfeature.R
import com.ekoapp.sample.chatfeature.data.ReactionData
import com.ekoapp.sample.chatfeature.dialogs.MessageBottomSheetFragment
import com.ekoapp.sample.chatfeature.messages.view.list.ReactionsAdapter
import com.ekoapp.sample.core.base.list.RecyclerBuilder
import com.ekoapp.sample.core.rx.into
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.component_image_message.view.*

class ImageMessageComponent : ConstraintLayout {

    init {
        LayoutInflater.from(context).inflate(R.layout.component_image_message, this, true)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun setMessage(item: EkoMessage, items: ArrayList<ReactionData>, reply: (EkoMessage) -> Unit, delete: (Boolean) -> Unit) {
        Glide.with(context).load(item.getData(ImageData::class.java).url)
                .placeholder(R.drawable.ic_placeholder_file)
                .into(image_message_content)
        val reactions = item.reactions.flatMap { result -> items.filter { result.key == it.name } }
        reactions.renderReactions()
        popupReactionAndReply(items, reply, item)
        renderMessageBottomSheet(delete)
    }

    private fun popupReactionAndReply(items: ArrayList<ReactionData>, reply: (EkoMessage) -> Unit, item: EkoMessage) {
        image_message_content.setOnLongClickListener {
            reaction_and_reply.visibility = View.VISIBLE
            return@setOnLongClickListener true
        }
        reaction_and_reply.setupView(items,
                selectedReaction = {
                    reaction_and_reply.visibility = View.GONE
                    item.react().addReaction(it).subscribe() into CompositeDisposable()
                },
                actionReply = {
                    reaction_and_reply.visibility = View.GONE
                    reply.invoke(item)
                })
    }

    private fun List<ReactionData>.renderReactions() {
        if (isNotEmpty()) {
            val adapter = ReactionsAdapter(context, this)
            RecyclerBuilder(context, recycler_reactions, size)
                    .builder()
                    .build(adapter)
            recycler_reactions.visibility = View.VISIBLE
        } else {
            recycler_reactions.visibility = View.GONE
        }
    }

    private fun renderMessageBottomSheet(delete: (Boolean) -> Unit) {
        image_more.setOnClickListener {
            val messageBottomSheet = MessageBottomSheetFragment()
            messageBottomSheet.show((context as AppCompatActivity).supportFragmentManager, messageBottomSheet.tag)

            messageBottomSheet.renderDelete {
                delete.invoke(it)
                messageBottomSheet.dialog?.cancel()
            }
        }
    }

    fun Boolean.showOrHideAvatar() {
        if (this) {
            avatar.visibility = View.INVISIBLE
        } else {
            avatar.visibility = View.VISIBLE
        }
    }
}