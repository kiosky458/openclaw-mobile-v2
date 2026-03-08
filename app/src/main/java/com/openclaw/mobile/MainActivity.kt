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

/**
 * MainActivity - Spark Agent Chat (測試版本，暫時移除 WebSocket)
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    
    private val messages = mutableListOf<ChatMessage>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        
        // 歡迎訊息
        addSystemMessage("OpenClaw Mobile - v1.2.1 (測試版)")
        addSystemMessage("UI 測試模式 - WebSocket 已暫時移除")
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
            title = "Spark Chat (測試)"
            subtitle = "✓ UI 測試模式"
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
        
        // 模擬回應
        addAgentMessage("收到訊息：$text\n\n（這是測試模式，WebSocket 功能已暫時移除）")
        
        // 清空輸入框
        inputField.text.clear()
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
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reconnect -> {
                addSystemMessage("測試模式 - 無需重連")
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
}
