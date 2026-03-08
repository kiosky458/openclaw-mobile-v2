package com.openclaw.mobile.websocket

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * HTTP Client - 使用 HTTP 輪詢替代 Socket.IO
 */
class WebSocketManager(
    private val serverUrl: String,
    private val listener: MessageListener
) {
    
    companion object {
        private const val TAG = "HttpClient"
        private const val POLL_INTERVAL = 1000L // 1秒輪詢一次
    }
    
    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val deviceId = "${Build.MANUFACTURER}-${Build.MODEL}-${System.currentTimeMillis()}"
    
    private var isConnected = false
    private var lastMessageId = 0.0
    private var pollRunnable: Runnable? = null
    
    interface MessageListener {
        fun onMessage(message: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    fun connect() {
        // 註冊裝置
        val registerData = mapOf(
            "device_id" to deviceId,
            "device_info" to mapOf(
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "android_version" to Build.VERSION.RELEASE
            )
        )
        
        val json = gson.toJson(registerData)
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$serverUrl/api/mobile/register")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    listener.onError("連接失敗：${e.message}")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    isConnected = true
                    handler.post {
                        listener.onConnected()
                    }
                    startPolling()
                } else {
                    handler.post {
                        listener.onError("註冊失敗")
                    }
                }
            }
        })
    }
    
    fun sendMessage(message: String): Boolean {
        if (!isConnected) {
            listener.onError("未連接")
            return false
        }
        
        val sendData = mapOf(
            "device_id" to deviceId,
            "message" to message
        )
        
        val json = gson.toJson(sendData)
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$serverUrl/api/mobile/send")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    listener.onError("發送失敗")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "Message sent successfully")
            }
        })
        
        return true
    }
    
    private fun startPolling() {
        pollRunnable = object : Runnable {
            override fun run() {
                if (!isConnected) return
                
                val pollData = mapOf(
                    "device_id" to deviceId,
                    "last_message_id" to lastMessageId
                )
                
                val json = gson.toJson(pollData)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url("$serverUrl/api/mobile/poll")
                    .post(body)
                    .build()
                
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // 忽略輪詢錯誤
                    }
                    
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseData = response.body?.string()
                            responseData?.let { parseMessages(it) }
                        }
                    }
                })
                
                handler.postDelayed(this, POLL_INTERVAL)
            }
        }
        
        handler.post(pollRunnable!!)
    }
    
    private fun parseMessages(json: String) {
        try {
            val data = gson.fromJson(json, PollResponse::class.java)
            
            data.messages.forEach { msg ->
                if (msg.id > lastMessageId) {
                    lastMessageId = msg.id
                    
                    handler.post {
                        listener.onMessage(msg.content)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse messages", e)
        }
    }
    
    fun disconnect() {
        isConnected = false
        pollRunnable?.let { handler.removeCallbacks(it) }
    }
    
    fun isConnected(): Boolean = isConnected
    
    data class PollResponse(
        val messages: List<Message>
    )
    
    data class Message(
        val id: Double,
        val content: String,
        val from: String,
        val timestamp: String
    )
}
