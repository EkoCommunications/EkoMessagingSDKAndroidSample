package com.ekoapp.sample.chatfeature.messages.view.list.viewholder

import android.view.View
import com.bumptech.glide.Glide
import com.ekoapp.sample.chatfeature.R
import com.ekoapp.sample.core.base.list.BaseViewHolder
import kotlinx.android.synthetic.main.item_reaction.view.*

class ReactionViewHolder(itemView: View) : BaseViewHolder<Int>(itemView) {

    override fun bind(item: Int) {
        val context = itemView.context
        Glide.with(context).load(item)
                .placeholder(R.drawable.ic_placeholder_file)
                .into(itemView.image_reaction)
    }
}