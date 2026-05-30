package com.deep.sideglance.domain.repository

import com.deep.sideglance.domain.model.UserStatus
import com.deep.sideglance.domain.model.StatusMessage
import com.deep.sideglance.domain.model.StatusReaction
import com.deep.sideglance.domain.model.User
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    suspend fun updateStatus(status: UserStatus): Result<Unit>
    suspend fun updateLocation(latitude: Double?, longitude: Double?): Result<Unit>
    fun observePartnerStatus(partnerUid: String): Flow<UserStatus?>
    fun observePartnerLocation(partnerUid: String): Flow<Pair<Double, Double>?>
    fun observePartnerUser(partnerUid: String): Flow<User?>
    suspend fun getPartnerStatus(partnerUid: String): UserStatus?
    suspend fun getPartnerUser(partnerUid: String): User?
    suspend fun addReactionToStatus(statusOwnerUid: String, reaction: StatusReaction): Result<Unit>
    suspend fun addMessageToStatus(statusOwnerUid: String, message: StatusMessage): Result<Unit>
}
