package com.ekoapp.simplechat.stream

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ekoapp.ekosdk.adapter.EkoPagedListAdapter
import com.ekoapp.ekosdk.stream.EkoStream
import com.ekoapp.simplechat.R
import com.google.common.base.Objects
import kotlinx.android.synthetic.main.item_stream.view.*

class StreamListAdapter : EkoPagedListAdapter<EkoStream, StreamListAdapter.StreamViewHolder>(STREAM_DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false)
        return StreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        val stream = getItem(position)
        if (stream == null) {
            holder.stream = null
            holder.itemView.stream_textview.text = "loading..."
        } else {
            val text = StringBuilder()
                    .append("id: ")
                    .append(stream.getStreamId())
                    .append("\ntitle: ")
                    .append(stream.getTitle())
                    .append("\ndescription : ")
                    .append(stream.getDescription())
                    .append("\n-------------------------------------------------")
                    .toString()
            holder.stream = stream
            holder.itemView.stream_textview.text = text
        }
    }

    class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var stream: EkoStream? = null

        init {
            itemView.setOnClickListener { view ->
                stream?.let {
                    val context = view.context
                    val intent = StreamVideoPlayerIntent(context, it.getStreamId())
                    context.startActivity(intent)
                }
            }
        }
    }
}


val STREAM_DIFF_CALLBACK: DiffUtil.ItemCallback<EkoStream> = object : DiffUtil.ItemCallback<EkoStream>() {
    override fun areItemsTheSame(oldEkoStream: EkoStream, newEkoStream: EkoStream): Boolean {
        return Objects.equal(oldEkoStream.getStreamId(), newEkoStream.getStreamId())
    }

    override fun areContentsTheSame(oldEkoStream: EkoStream, newEkoStream: EkoStream): Boolean {
        return (Objects.equal(oldEkoStream.getStreamId(), newEkoStream.getStreamId())
                && Objects.equal(oldEkoStream.getTitle(), newEkoStream.getTitle())
                && Objects.equal(oldEkoStream.getDescription(), newEkoStream.getDescription()))
    }
}