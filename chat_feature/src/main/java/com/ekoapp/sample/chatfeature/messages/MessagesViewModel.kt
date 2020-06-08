package com.ekoapp.sample.chatfeature.messages

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.ekoapp.ekosdk.EkoMessage
import com.ekoapp.ekosdk.EkoTags
import com.ekoapp.sample.chatfeature.data.MessageData
import com.ekoapp.sample.chatfeature.data.NotificationData
import com.ekoapp.sample.chatfeature.data.SendMessageData
import com.ekoapp.sample.chatfeature.repositories.ChannelRepository
import com.ekoapp.sample.chatfeature.repositories.MessageRepository
import com.ekoapp.sample.chatfeature.repositories.UserRepository
import com.ekoapp.sample.core.base.viewmodel.DisposableViewModel
import com.ekoapp.sample.core.ui.extensions.toLiveData
import com.ekoapp.sample.core.utils.stringToSet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MessagesViewModel @Inject constructor(private val context: Context,
                                            private val channelRepository: ChannelRepository,
                                            private val messageRepository: MessageRepository,
                                            private val userRepository: UserRepository) : DisposableViewModel() {

    private val notificationRelay = PublishProcessor.create<NotificationData>()

    fun observeNotification() = notificationRelay.toLiveData()

    fun bindStartReading(channelId: String) = channelRepository.startReading(channelId)

    fun bindStopReading(channelId: String) = channelRepository.stopReading(channelId)

    fun bindGetMessageCollectionByTags(data: MessageData): LiveData<PagedList<EkoMessage>> {
        return messageRepository.getMessageCollectionByTags(data)
    }

    fun bindTextMessage(data: SendMessageData) {
        messageRepository.textMessage(data)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    fun bindSetTagsChannel(channelId: String, tags: String) {
        channelRepository.setTags(channelId, EkoTags(tags.stringToSet()))
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun bindSetTagsMessage(messageId: String, tags: String) {
        messageRepository.setTags(messageId, EkoTags(tags.stringToSet()))
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun bindNotification(channelId: String) {
        channelRepository.notification(channelId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(notificationRelay::onNext)
                .subscribe()
    }

    fun bindSetNotification(data: NotificationData) {
        channelRepository.setNotification(data.channelId, data.isAllowed)
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun bindUnFlagMessage(messageId: String) {
        messageRepository.unFlag(messageId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    fun bindFlagMessage(messageId: String) {
        messageRepository.flag(messageId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    fun bindUnFlagUser(userId: String) {
        userRepository.unFlag(userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    fun bindFlagUser(userId: String) {
        userRepository.flag(userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

}