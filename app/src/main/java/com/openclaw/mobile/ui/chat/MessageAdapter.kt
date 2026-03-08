package com.openclaw.mobile.ui.chat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.data.ChatMessage

class MessageAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        init {
            // 黑底設定
            itemView.setBackgroundColor(Color.parseColor("#000000"))
            textView.setTextColor(Color.parseColor("#FFFFFF"))
        }
        
        fun bind(message: ChatMessage) {
            val prefix = when {
                message.isSystem -> "💬"
                message.isFromUser -> "👤"
                else -> "🤖"
            }
            textView.text = "$prefix ${message.content}"
        }
    }
}
