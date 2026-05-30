package com.deep.sideglance.domain.model

data class StatusMessage(
    val senderUid: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StatusReaction(
    val senderUid: String = "",
    val emoji: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class UserStatus(
    val statusType: String, // "Work", "Cook", "Play", etc.
    val detail: String? = null, // e.g. "Football", "Cricket"
    val timestamp: Long = System.currentTimeMillis(),
    val expirationTime: Long = 0L, // 0L means forever, otherwise a timestamp
    val messages: List<StatusMessage> = emptyList(),
    val reactions: List<StatusReaction> = emptyList()
) {
    val isExpired: Boolean
        get() = expirationTime != 0L && System.currentTimeMillis() > expirationTime
}

