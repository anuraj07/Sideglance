package com.deep.sideglance.data.model

import com.deep.sideglance.domain.model.UserStatus
import com.deep.sideglance.domain.model.StatusMessage
import com.deep.sideglance.domain.model.StatusReaction

data class FirestoreStatus(
    val statusType: String = "",
    val detail: String? = null,
    val timestamp: Long = 0L,
    val expirationTime: Long = 0L,
    val messages: List<FirestoreStatusMessage> = emptyList(),
    val reactions: List<FirestoreStatusReaction> = emptyList()
) {
    fun toDomain(): UserStatus = UserStatus(
        statusType = statusType,
        detail = detail,
        timestamp = timestamp,
        expirationTime = expirationTime,
        messages = messages.map { it.toDomain() },
        reactions = reactions.map { it.toDomain() }
    )

    companion object {
        fun fromDomain(status: UserStatus): FirestoreStatus = FirestoreStatus(
            statusType = status.statusType,
            detail = status.detail,
            timestamp = status.timestamp,
            expirationTime = status.expirationTime,
            messages = status.messages.map { FirestoreStatusMessage.fromDomain(it) },
            reactions = status.reactions.map { FirestoreStatusReaction.fromDomain(it) }
        )
    }
}

data class FirestoreStatusMessage(
    val senderUid: String = "",
    val text: String = "",
    val timestamp: Long = 0L
) {
    fun toDomain(): StatusMessage = StatusMessage(
        senderUid = senderUid,
        text = text,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(msg: StatusMessage): FirestoreStatusMessage = FirestoreStatusMessage(
            senderUid = msg.senderUid,
            text = msg.text,
            timestamp = msg.timestamp
        )
    }
}

data class FirestoreStatusReaction(
    val senderUid: String = "",
    val emoji: String = "",
    val timestamp: Long = 0L
) {
    fun toDomain(): StatusReaction = StatusReaction(
        senderUid = senderUid,
        emoji = emoji,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(react: StatusReaction): FirestoreStatusReaction = FirestoreStatusReaction(
            senderUid = react.senderUid,
            emoji = react.emoji,
            timestamp = react.timestamp
        )
    }
}

