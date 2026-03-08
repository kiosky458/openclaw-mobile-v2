package com.openclaw.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.ui.chat.MessageAdapter

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputField: EditText
    private lateinit var sendButton: Button
    
    private val messages = mutableListOf<ChatMessage>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        recyclerView = findViewById(R.id.messages_recycler_view)
        inputField = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
        
        setupRecyclerView()
        setupListeners()
        
        // 初始訊息
        addSystemMessage("v1.0.2 測試版")
        addSystemMessage("RecyclerView + 簡單 UI")
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
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                addAgentMessage("收到：$text")
                inputField.text.clear()
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
}
