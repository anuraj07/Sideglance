package com.deep.sideglance.data.model

import com.deep.sideglance.domain.model.UserStatus

data class FirestoreStatus(
    val statusType: String = "",
    val detail: String? = null,
    val timestamp: Long = 0L,
    val expirationTime: Long = 0L
) {
    fun toDomain(): UserStatus = UserStatus(
        statusType = statusType,
        detail = detail,
        timestamp = timestamp,
        expirationTime = expirationTime
    )

    companion object {
        fun fromDomain(status: UserStatus): FirestoreStatus = FirestoreStatus(
            statusType = status.statusType,
            detail = status.detail,
            timestamp = status.timestamp,
            expirationTime = status.expirationTime
        )
    }
}
