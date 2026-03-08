package com.openclaw.mobile.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.R
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
        
        fun bind(message: ChatMessage) {
            val prefix = when {
                message.isSystem -> "[系統]"
                message.isFromUser -> "[我]"
                else -> "[Agent]"
            }
            textView.text = "$prefix ${message.content}"
        }
    }
}
