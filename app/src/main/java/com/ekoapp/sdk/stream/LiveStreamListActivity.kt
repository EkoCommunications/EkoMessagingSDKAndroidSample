package com.ekoapp.sdk.stream

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.paging.PagedList
import com.ekoapp.ekosdk.EkoClient
import com.ekoapp.ekosdk.sdk.BuildConfig
import com.ekoapp.ekosdk.stream.EkoStream
import com.ekoapp.sdk.R
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_channel_list.toolbar
import kotlinx.android.synthetic.main.activity_stream_list.*
import java.util.concurrent.TimeUnit

open class LiveStreamListActivity : AppCompatActivity() {

    private var streamDisposable: Disposable? = null
    private val streamRepository = EkoClient.newStreamRepository()

    private var adapter: StreamListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_list)
        setupToolbar()
        observeStreamCollection()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (streamDisposable?.isDisposed == false) {
            streamDisposable?.dispose()
        }
    }

    open protected fun getStatus(): Array<EkoStream.Status> = arrayOf(EkoStream.Status.LIVE)

    open protected fun subtitle(): String = "Live Stream"

    private fun setupToolbar() {
        val appName = getString(R.string.app_name)
        toolbar.title = String.format("%s %s: %s", appName, "Eko SDK", BuildConfig.VERSION_NAME)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setSubtitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        setSupportActionBar(toolbar)
    }

    private fun observeStreamCollection() {
        adapter = StreamListAdapter()
        stream_list_recyclerview.adapter = adapter
        streamDisposable = getLiveStreams()
                .throttleLatest(1, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCollectionUpdated(it) }, { })
    }

    private fun onCollectionUpdated(pagedList: PagedList<EkoStream>) {
        adapter?.submitList(pagedList)
        val isEmpty = pagedList.isNullOrEmpty()
        if (isEmpty) {
            stream_list_recyclerview.visibility = View.GONE
            stream_empty_state_container.visibility = View.VISIBLE
        } else {
            stream_list_recyclerview.visibility = View.VISIBLE
            stream_empty_state_container.visibility = View.GONE
        }
    }

    private fun getLiveStreams(): Flowable<PagedList<EkoStream>> {
        return streamRepository
                .getStreamCollection()
                .setStatus(getStatus())
                .build()
                .query()
    }
}