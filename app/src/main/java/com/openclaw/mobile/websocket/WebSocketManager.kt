package com.openclaw.mobile.websocket

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

/**
 * WebSocketManager - Socket.IO 連接管理器
 */
class WebSocketManager(
    private val serverUrl: String,
    private val listener: MessageListener
) {
    
    companion object {
        private const val TAG = "WebSocketManager"
    }
    
    private var socket: Socket? = null
    private var isManualDisconnect = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val deviceId = "${Build.MANUFACTURER}-${Build.MODEL}-${System.currentTimeMillis()}"
    
    interface MessageListener {
        fun onMessage(message: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    /**
     * 連接 Socket.IO
     */
    fun connect() {
        if (socket != null && socket!!.connected()) {
            Log.d(TAG, "Already connected")
            return
        }
        
        isManualDisconnect = false
        
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                reconnectionAttempts = Int.MAX_VALUE
            }
            
            socket = IO.socket(serverUrl, opts)
            
            // 連接成功
            socket!!.on(Socket.EVENT_CONNECT, onConnect)
            
            // 斷線
            socket!!.on(Socket.EVENT_DISCONNECT, onDisconnect)
            
            // 連接錯誤
            socket!!.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            
            // 註冊成功
            socket!!.on("mobile_registered", onRegistered)
            
            // Agent 訊息（即時串流）
            socket!!.on("agent_message", onAgentMessage)
            
            // 錯誤訊息
            socket!!.on("error", onError)
            
            Log.d(TAG, "Connecting to $serverUrl")
            socket!!.connect()
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid URL: $serverUrl", e)
            mainHandler.post {
                listener.onError("無效的伺服器位址")
            }
        }
    }
    
    /**
     * 連接成功事件
     */
    private val onConnect = Emitter.Listener {
        Log.d(TAG, "Connected")
        
        // 發送註冊訊息
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
    
    /**
     * 註冊成功事件
     */
    private val onRegistered = Emitter.Listener { args ->
        Log.d(TAG, "Registered: ${args[0]}")
        mainHandler.post {
            listener.onConnected()
        }
    }
    
    /**
     * 斷線事件
     */
    private val onDisconnect = Emitter.Listener { args ->
        Log.d(TAG, "Disconnected: ${args.joinToString()}")
        mainHandler.post {
            listener.onDisconnected()
        }
    }
    
    /**
     * 連接錯誤事件
     */
    private val onConnectError = Emitter.Listener { args ->
        Log.e(TAG, "Connection error: ${args.joinToString()}")
        mainHandler.post {
            listener.onError("連接失敗")
        }
    }
    
    /**
     * Agent 訊息事件
     */
    private val onAgentMessage = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val message = data.getString("message")
            val isDelta = data.optBoolean("delta", false)
            val isDone = data.optBoolean("done", false)
            
            Log.d(TAG, "Agent message: $message (delta=$isDelta, done=$isDone)")
            
            if (!isDone && message.isNotEmpty()) {
                mainHandler.post {
                    listener.onMessage(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse agent message", e)
        }
    }
    
    /**
     * 錯誤事件
     */
    private val onError = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val errorMsg = data.getString("message")
            Log.e(TAG, "Error: $errorMsg")
            mainHandler.post {
                listener.onError(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse error", e)
        }
    }
    
    /**
     * 發送訊息
     */
    fun sendMessage(message: String): Boolean {
        val s = socket
        if (s == null || !s.connected()) {
            Log.w(TAG, "Cannot send message: not connected")
            mainHandler.post {
                listener.onError("未連接")
            }
            return false
        }
        
        Log.d(TAG, "Sending message: $message")
        
        val data = JSONObject().apply {
            put("message", message)
        }
        
        s.emit("mobile_message", data)
        return true
    }
    
    /**
     * 斷開連接
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting")
        isManualDisconnect = true
        socket?.disconnect()
        socket?.off()
        socket = null
    }
    
    /**
     * 重新連接
     */
    fun reconnect() {
        Log.d(TAG, "Reconnecting")
        disconnect()
        mainHandler.postDelayed({
            connect()
        }, 500)
    }
    
    /**
     * 是否已連接
     */
    fun isConnected(): Boolean = socket?.connected() == true
}
