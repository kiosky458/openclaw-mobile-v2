package com.openclaw.mobile

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.ui.chat.MessageAdapter
import com.openclaw.mobile.websocket.WebSocketManager

/**
 * MainActivity - Spark Agent Chat
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var webSocketManager: WebSocketManager
    
    private val messages = mutableListOf<ChatMessage>()
    
    // WebSocket 端點（可在設定中修改）
    private val wsUrl = "https://artiforge.studio"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupWebSocket()
        setupListeners()
        
        // 歡迎訊息
        addSystemMessage("OpenClaw Mobile - v1.2.0")
        addSystemMessage("連接至 Spark Agent...")
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.messages_recycler_view)
        inputField = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Spark Chat"
            subtitle = "連接中..."
        }
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
        webSocketManager = WebSocketManager(wsUrl, object : WebSocketManager.MessageListener {
            override fun onMessage(message: String) {
                addAgentMessage(message)
            }
            
            override fun onConnected() {
                addSystemMessage("已連接")
                updateConnectionStatus(true)
            }
            
            override fun onDisconnected() {
                addSystemMessage("連接中斷")
                updateConnectionStatus(false)
            }
            
            override fun onError(error: String) {
                addSystemMessage("⚠️ $error")
            }
        })
        
        webSocketManager.connect()
    }
    
    private fun setupListeners() {
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        inputField.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }
    
    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return
        
        // 顯示用戶訊息
        addUserMessage(text)
        
        // 發送到 WebSocket
        if (webSocketManager.sendMessage(text)) {
            inputField.text.clear()
        } else {
            addSystemMessage("⚠️ 發送失敗")
        }
    }
    
    private fun addUserMessage(text: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addAgentMessage(text: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addSystemMessage(text: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            isSystem = true
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun updateConnectionStatus(connected: Boolean) {
        supportActionBar?.subtitle = if (connected) "✓ 已連接" else "✗ 未連接"
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reconnect -> {
                addSystemMessage("重新連接中...")
                webSocketManager.reconnect()
                true
            }
            R.id.action_clear -> {
                messages.clear()
                messageAdapter.notifyDataSetChanged()
                addSystemMessage("訊息已清除")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}
