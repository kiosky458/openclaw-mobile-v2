package com.openclaw.mobile.data

data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isSystem: Boolean = false
)
