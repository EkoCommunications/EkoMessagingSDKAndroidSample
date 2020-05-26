package com.ekoapp.sample.socialfeature.userfeeds.view.list


import android.view.LayoutInflater
import android.view.ViewGroup
import com.ekoapp.ekosdk.adapter.EkoPostAdapter
import com.ekoapp.sample.core.base.list.ViewHolder
import com.ekoapp.sample.socialfeature.R
import com.ekoapp.sample.socialfeature.enums.ReactionTypes
import com.ekoapp.sample.socialfeature.reactions.data.UserReactionData
import com.ekoapp.sample.socialfeature.userfeeds.view.UserFeedsViewModel
import com.ekoapp.sample.socialfeature.userfeeds.view.renders.EkoUserFeedsRenderData
import com.ekoapp.sample.socialfeature.userfeeds.view.renders.ReactionData
import com.ekoapp.sample.socialfeature.userfeeds.view.renders.userFeedRender
import kotlinx.android.synthetic.main.item_user_feeds.view.*

class EkoUserFeedsAdapter(private val userFeedsViewModel: UserFeedsViewModel) : EkoPostAdapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user_feeds, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val itemView = holder.itemView
        val context = itemView.context

        item?.apply {
            EkoUserFeedsRenderData(context, this).userFeedRender(
                    header = itemView.header_feeds,
                    body = itemView.body_feeds,
                    reactionsSummary = itemView.reactions_summary,
                    footer = itemView.footer_feeds,
                    eventFavorite = {
                        userFeedsViewModel.reactionFeeds(ReactionData(text = ReactionTypes.FAVORITE.text, isChecked = it, item = item))
                    },
                    eventReactionsSummary = {
                        userFeedsViewModel.reactionsSummaryActionRelay.postValue(UserReactionData(postId = item.postId))
                    },
                    eventLike = {
                        userFeedsViewModel.reactionFeeds(ReactionData(text = ReactionTypes.LIKE.text, isChecked = it, item = item))
                    },
                    eventEdit = userFeedsViewModel.editFeedsActionRelay::postValue,
                    eventDelete = {
                        userFeedsViewModel.deletePost(this)
                    })
        }
    }
}

