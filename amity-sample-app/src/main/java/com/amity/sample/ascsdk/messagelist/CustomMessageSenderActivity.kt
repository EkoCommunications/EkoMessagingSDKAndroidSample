package com.amity.sample.ascsdk.messagelist

import android.app.Activity
import android.os.Bundle
import com.amity.socialcloud.sdk.chat.AmityChatClient

import com.amity.sample.ascsdk.common.activity.KeyValueInputActivity
import com.amity.sample.ascsdk.intent.OpenCustomMessageSenderIntent
import com.google.gson.JsonObject
import io.reactivex.Completable
import kotlinx.android.synthetic.main.activity_key_value_input.*

class CustomMessageSenderActivity : KeyValueInputActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Send custom"
    }

    override fun onButtonClick() {
        send_button.isEnabled = false
        sendCustomMessage()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun sendCustomMessage() {
        val request = createRequest()
        request.subscribe()
    }

    private fun createRequest(): Completable {
        val messageRepository = AmityChatClient.newMessageRepository()
        val channelId = OpenCustomMessageSenderIntent.getChannelId(intent) ?: ""
        val parentId = OpenCustomMessageSenderIntent.getParentId(intent)
        val data = JsonObject();
        data.addProperty(key_edittext.text.toString().trim(), value_edittext.text.toString().trim());

        return messageRepository
                .createMessage(channelId)
                .parentId(parentId)
                .with()
                .custom(data)
                .build()
                .send()

    }

}