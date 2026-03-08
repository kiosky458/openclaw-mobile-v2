package com.openclaw.mobile.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocketManager - WebSocket 連接管理器
 */
class WebSocketManager(
    private val wsUrl: String,
    private val listener: MessageListener
) {
    
    companion object {
        private const val TAG = "WebSocketManager"
    }
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private var isManualDisconnect = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    interface MessageListener {
        fun onMessage(message: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    /**
     * 連接 WebSocket
     */
    fun connect() {
        if (webSocket != null) {
            Log.d(TAG, "Already connected")
            return
        }
        
        isManualDisconnect = false
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        Log.d(TAG, "Connecting to $wsUrl")
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected")
                mainHandler.post {
                    listener.onConnected()
                }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                mainHandler.post {
                    listener.onMessage(text)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing: $code - $reason")
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed: $code - $reason")
                this@WebSocketManager.webSocket = null
                mainHandler.post {
                    listener.onDisconnected()
                }
                
                // 自動重連（如果不是手動斷線）
                if (!isManualDisconnect) {
                    reconnectAfterDelay()
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed: ${t.message}", t)
                this@WebSocketManager.webSocket = null
                mainHandler.post {
                    listener.onError(t.message ?: "連接失敗")
                }
                
                // 自動重連
                if (!isManualDisconnect) {
                    reconnectAfterDelay()
                }
            }
        })
    }
    
    /**
     * 發送訊息
     */
    fun sendMessage(message: String): Boolean {
        val ws = webSocket
        if (ws == null) {
            Log.w(TAG, "Cannot send message: not connected")
            mainHandler.post {
                listener.onError("未連接")
            }
            return false
        }
        
        Log.d(TAG, "Sending message: $message")
        return ws.send(message)
    }
    
    /**
     * 斷開連接
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting")
        isManualDisconnect = true
        webSocket?.close(1000, "手動斷開")
        webSocket = null
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
     * 延遲重連（5 秒後）
     */
    private fun reconnectAfterDelay() {
        mainHandler.postDelayed({
            if (!isManualDisconnect && webSocket == null) {
                Log.d(TAG, "Auto reconnecting...")
                listener.onError("5秒後重新連接...")
                connect()
            }
        }, 5000)
    }
    
    /**
     * 是否已連接
     */
    fun isConnected(): Boolean = webSocket != null
}
