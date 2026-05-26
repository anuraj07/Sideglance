package com.deep.sideglance.data.repository

import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.model.UserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

object MockDatabase {
    // Current authenticated user (locally signed in)
    val currentUser = MutableStateFlow<User?>(null)

    // In-memory storage for users: UID -> User
    val users = MutableStateFlow<Map<String, User>>(emptyMap())

    // In-memory storage for statuses: UID -> UserStatus
    val statuses = MutableStateFlow<Map<String, UserStatus>>(emptyMap())

    // Helper: Reset mock db for tests
    fun reset() {
        currentUser.value = null
        users.value = emptyMap()
        statuses.value = emptyMap()
    }

    // Initialize mock data for pairing demo
    fun seedMockPartner(selfUid: String) {
        val partnerUid = "mock_partner_123"
        val partner = User(
            uid = partnerUid,
            email = "partner@example.com",
            displayName = "Taylor (Partner)",
            photoUrl = null,
            partnerUid = selfUid,
            latitude = 37.7749, // San Francisco coordinates
            longitude = -122.4194,
            locationTimestamp = System.currentTimeMillis()
        )
        val partnerStatus = UserStatus(
            statusType = "Cook",
            detail = "Baking chocolate chip cookies 🍪",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 mins ago
            expirationTime = System.currentTimeMillis() + 1000 * 60 * 60 * 4 // Expires in 4 hours
        )

        val updatedMap = users.value.toMutableMap()
        updatedMap[partnerUid] = partner
        users.value = updatedMap

        val updatedStatusMap = statuses.value.toMutableMap()
        updatedStatusMap[partnerUid] = partnerStatus
        statuses.value = updatedStatusMap
    }
}
