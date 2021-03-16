package com.ekoapp.sdk.stream

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ekoapp.sdk.R
import com.ekoapp.sdk.common.extensions.showToast
import com.ekoapp.sdk.publisher.EkoStreamBroadcaster
import com.ekoapp.sdk.publisher.EkoStreamBroadcasterConfiguration
import com.ekoapp.sdk.publisher.StreamBroadcaster
import com.ekoapp.sdk.publisher.model.EkoBroadcastResolution
import com.ekoapp.sdk.publisher.model.EkoStreamBroadcasterState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.flowable.FlowableInterval
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_streamer.*
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit


class StreamerActivity : AppCompatActivity() {

    private var durationDisposable: Disposable? = null
    private var streamBroadcaster: StreamBroadcaster? = null
    private var broadcasterConfig = EkoStreamBroadcasterConfiguration.Builder()
            .setOrientation(Configuration.ORIENTATION_PORTRAIT)
            .setResolution(EkoBroadcastResolution.HD_720P)
            .build()

    private var duration = 0L
    private var streamBroadcasterState: EkoStreamBroadcasterState = EkoStreamBroadcasterState.IDLE()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_streamer)
        requestPermission()
    }

    private fun requestPermission() {
        RxPermissions(this)
                .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { granted ->
                    if (granted) {
                        setupView()
                        initBroadcaster()
                    } else {
                        showPermissionErrorDialog()
                    }
                }
    }

    private fun showResolutionOption() {
        val builderSingle: AlertDialog.Builder = AlertDialog.Builder(this)
        builderSingle.setTitle("Select video resolution")
        builderSingle.setNegativeButton("cancel") { dialog, _ -> dialog.dismiss() }
        val resolutions = EkoBroadcastResolution.values()

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
        EkoBroadcastResolution.values().forEachIndexed { _, resolution ->
            val readableName = resolution.readableName
            val text = "\n$readableName\n"
            arrayAdapter.add(text)
        }

        builderSingle.setAdapter(arrayAdapter) { _, which ->
            broadcasterConfig.resolution = resolutions[which]
            streamBroadcaster?.updateConfiguration(broadcasterConfig)
            resolution_textview.text = "Current Resolution : " + resolutions[which].readableName
        }
        builderSingle.show()
    }

    private fun initBroadcaster() {
        streamBroadcaster = EkoStreamBroadcaster.Builder(eko_camera)
                .setConfiguration(broadcasterConfig)
                .build()
        streamBroadcaster?.startPreview()
        subscribeBroadcastStatus()
    }


    private fun setupView() {
        toggle_settings.setOnClickListener { showResolutionOption() }
        toggle_camera.setOnClickListener { streamBroadcaster?.switchCamera() }
        icon_close.setOnClickListener { finish() }
        toggle_publish.setOnClickListener {
            if (toggle_publish.text.toString().contentEquals("Start Broadcasting")) {
                startStreaming()
            } else if (toggle_publish.text.toString().contentEquals("finish")) {
                showStopStreamingDialog()
            }
        }
    }

    private fun showStopStreamingDialog() {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.stop_streaming_title)
                .setMessage(R.string.stop_streaming_description)
                .setNeutralButton(R.string.general_cancel) { _, _ -> }
                .setNegativeButton(R.string.general_stop) { _, _ -> stopStreaming() }
                .show()
    }

    private fun showPermissionErrorDialog() {
        MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.general_warning)
                .setMessage(R.string.permission_error)
                .setNeutralButton(R.string.understand) { _, _ -> finish() }
                .show()
    }

    private fun subscribeBroadcastStatus() {
        streamBroadcaster?.stateFlowable
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.doOnNext {
                    streamBroadcasterState = it
                    when (it) {
                        is EkoStreamBroadcasterState.CONNECTING -> {
                            showReconnectLabel()
                        }
                        is EkoStreamBroadcasterState.DISCONNECTED -> {
                            showDisconnectError(it.getError())
                        }
                    }
                }
                ?.subscribe()
    }

    private fun startStreaming() {
        streamBroadcaster?.startPublish(getVideoTitle(), getVideoDescription())
        toggle_publish.text = "finish"
        creation_container.visibility = View.GONE
        showReconnectLabel()
        startCounting()
    }

    private fun stopStreaming() {
        streamBroadcaster?.stopPublish()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        streamBroadcaster?.stopPublish()
        if (durationDisposable?.isDisposed == false) {
            durationDisposable?.dispose()
        }
    }


    private fun getVideoDescription(): String {
        val day = DateTime.now().dayOfMonth().get()
        val month = DateTime.now().monthOfYear().get()
        val year = DateTime.now().year().get()
        val default = "Date : $day/$month/$year"
        val inputText = description_edittext.text.toString()
        return if (inputText.isNotEmpty()) inputText else default
    }

    private fun getVideoTitle(): String {
        val hour = DateTime.now().hourOfDay().get()
        val minute = DateTime.now().minuteOfHour().get()
        val default = "Time : $hour : $minute"
        val inputText = title_edittext.text.toString()
        return if (inputText.isNotEmpty()) inputText else default
    }

    private fun showReconnectLabel() {
        creation_container.visibility = View.GONE
        live_label.text = "Connecting"
        live_label.visibility = View.VISIBLE
    }

    private fun showDisconnectError(exception: Exception?) {
        showToast("Rtmp error : " + exception?.message)
    }

    private fun startCounting() {
        durationDisposable = FlowableInterval(0, 1, TimeUnit.SECONDS, Schedulers.io())
                .filter { streamBroadcasterState is EkoStreamBroadcasterState.CONNECTED }
                .map { duration++ }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    val durationMs = it * 1000
                    val second = durationMs / 1000 % 60
                    val min = durationMs / 1000 / 60
                    live_label.text = "LIVE " + min.format() + ":" + second.format()
                }
                .subscribe()
    }

    private fun Long.format(): String {
        return String.format("%02d", this)
    }
}