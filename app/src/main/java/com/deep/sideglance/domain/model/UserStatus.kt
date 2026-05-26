package com.deep.sideglance.domain.model

data class UserStatus(
    val statusType: String, // "Work", "Cook", "Play", etc.
    val detail: String? = null, // e.g. "Football", "Cricket"
    val timestamp: Long = System.currentTimeMillis(),
    val expirationTime: Long = 0L // 0L means forever, otherwise a timestamp
) {
    val isExpired: Boolean
        get() = expirationTime != 0L && System.currentTimeMillis() > expirationTime
}
