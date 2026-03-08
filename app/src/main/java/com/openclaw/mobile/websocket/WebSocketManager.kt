package com.openclaw.mobile.websocket

import android.os.Build
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

class WebSocketManager(
    private val serverUrl: String,
    private val listener: MessageListener
) {
    
    companion object {
        private const val TAG = "WebSocketManager"
    }
    
    private var socket: Socket? = null
    private val deviceId = "${Build.MANUFACTURER}-${Build.MODEL}-${System.currentTimeMillis()}"
    
    interface MessageListener {
        fun onMessage(message: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    fun connect() {
        try {
            // 關鍵：設置 IO.Options（參考 android-stream-relay）
            val options = IO.Options()
            options.forceNew = true
            options.reconnection = true
            options.reconnectionDelay = 1000
            options.reconnectionDelayMax = 5000
            options.reconnectionAttempts = Int.MAX_VALUE
            
            socket = IO.socket(serverUrl, options)
            
            socket!!.on(Socket.EVENT_CONNECT, onConnect)
            socket!!.on(Socket.EVENT_DISCONNECT, onDisconnect)
            socket!!.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            socket!!.on("mobile_registered", onRegistered)
            socket!!.on("agent_message", onAgentMessage)
            socket!!.on("error", onError)
            
            socket!!.connect()
            Log.d(TAG, "Connecting to $serverUrl")
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid URL: $serverUrl", e)
            listener.onError("無效的伺服器位址")
        }
    }
    
    private val onConnect = Emitter.Listener {
        Log.d(TAG, "Connected")
        
        val registerData = JSONObject().apply {
            put("device_id", deviceId)
            put("device_info", JSONObject().apply {
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("android_version", Build.VERSION.RELEASE)
            })
        }
        
        socket?.emit("mobile_register", registerData)
    }
    
    private val onRegistered = Emitter.Listener {
        Log.d(TAG, "Registered")
        listener.onConnected()
    }
    
    private val onDisconnect = Emitter.Listener {
        Log.d(TAG, "Disconnected")
        listener.onDisconnected()
    }
    
    private val onConnectError = Emitter.Listener { args ->
        Log.e(TAG, "Connection error: ${args.joinToString()}")
        listener.onError("連接失敗")
    }
    
    private val onAgentMessage = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val message = data.getString("message")
            val isDone = data.optBoolean("done", false)
            
            if (!isDone && message.isNotEmpty()) {
                listener.onMessage(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse agent message", e)
        }
    }
    
    private val onError = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val errorMsg = data.getString("message")
            listener.onError(errorMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse error", e)
        }
    }
    
    fun sendMessage(message: String): Boolean {
        val s = socket
        if (s == null || !s.connected()) {
            listener.onError("未連接")
            return false
        }
        
        val data = JSONObject().apply {
            put("message", message)
        }
        
        s.emit("mobile_message", data)
        return true
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
    
    fun isConnected(): Boolean = socket?.connected() == true
}
