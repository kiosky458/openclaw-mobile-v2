package com.openclaw.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.ui.chat.MessageAdapter
import com.openclaw.mobile.websocket.WebSocketManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputField: EditText
    private lateinit var sendButton: Button
    private lateinit var webSocketManager: WebSocketManager
    
    private val messages = mutableListOf<ChatMessage>()
    private val serverUrl = "https://artiforge.studio"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.status_text)
        recyclerView = findViewById(R.id.messages_recycler_view)
        inputField = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
        
        setupRecyclerView()
        setupWebSocket()
        setupListeners()
        
        addSystemMessage("OpenClaw Mobile v1.1.0-final")
        addSystemMessage("連接至 Spark Agent...")
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }
    
    private fun setupWebSocket() {
        webSocketManager = WebSocketManager(serverUrl, object : WebSocketManager.MessageListener {
            override fun onMessage(message: String) {
                runOnUiThread {
                    addAgentMessage(message)
                }
            }
            
            override fun onConnected() {
                runOnUiThread {
                    statusText.text = "✓ 已連接"
                    statusText.setTextColor(0xFF00FF00.toInt())
                    addSystemMessage("已連接至 Spark")
                }
            }
            
            override fun onDisconnected() {
                runOnUiThread {
                    statusText.text = "✗ 未連接"
                    statusText.setTextColor(0xFFFF0000.toInt())
                    addSystemMessage("連接中斷")
                }
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    addSystemMessage("⚠️ $error")
                }
            }
        })
        
        webSocketManager.connect()
    }
    
    private fun setupListeners() {
        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                if (webSocketManager.sendMessage(text)) {
                    inputField.text.clear()
                }
            }
        }
    }
    
    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        ))
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addAgentMessage(text: String) {
        messages.add(ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        ))
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addSystemMessage(text: String) {
        messages.add(ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            isSystem = true
        ))
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}
