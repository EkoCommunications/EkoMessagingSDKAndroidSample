package com.ekoapp.sample.chatfeature.repositories

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.ekoapp.ekosdk.EkoChannel
import com.ekoapp.ekosdk.EkoChannelFilter
import com.ekoapp.ekosdk.EkoClient
import com.ekoapp.ekosdk.EkoTags
import com.google.common.collect.ImmutableSet
import io.reactivex.Completable
import io.reactivex.Flowable
import javax.inject.Inject

class ChannelRepository @Inject constructor() {

    val channelTypes = setOf(EkoChannel.Type.STANDARD,
            EkoChannel.Type.PRIVATE,
            EkoChannel.Type.BROADCAST,
            EkoChannel.Type.CONVERSATION)

    val channelFilters = setOf(EkoChannelFilter.ALL,
            EkoChannelFilter.MEMBER,
            EkoChannelFilter.NOT_MEMBER)

    fun getTotalUnreadCount(): Flowable<Int> {
        return EkoClient.newChannelRepository().totalUnreadCount
    }

    fun createChannel(channelId: String, type: String): Completable {
        return EkoClient.newChannelRepository().createChannel(channelId,
                EkoChannel.CreationType.fromJson(type),
                EkoChannel.CreateOption.none())
    }

    fun createConversation(userId: String): Completable {
        return EkoClient.newChannelRepository().createConversation(userId)
    }

    fun joinChannel(channelId: String): Completable {
        return EkoClient.newChannelRepository().joinChannel(channelId)
    }

    fun channelCollection(types: ImmutableSet<EkoChannel.Type>,
                          filter: EkoChannelFilter,
                          includingTags: EkoTags,
                          excludingTags: EkoTags): LiveData<PagedList<EkoChannel>> {

        return EkoClient.newChannelRepository().channelCollection
                .byTypes()
                .types(types)
                .filter(filter)
                .includingTags(includingTags)
                .excludingTags(excludingTags)
                .build()
                .query()
    }

}