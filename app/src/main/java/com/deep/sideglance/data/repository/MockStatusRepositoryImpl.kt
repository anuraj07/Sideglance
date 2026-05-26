package com.deep.sideglance.data.repository

import com.deep.sideglance.domain.model.UserStatus
import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MockStatusRepositoryImpl : StatusRepository {
    override suspend fun updateStatus(status: UserStatus): Result<Unit> {
        val current = MockDatabase.currentUser.value ?: return Result.failure(Exception("Not logged in"))
        
        val updatedStatuses = MockDatabase.statuses.value.toMutableMap()
        updatedStatuses[current.uid] = status
        MockDatabase.statuses.value = updatedStatuses
        
        return Result.success(Unit)
    }

    override suspend fun updateLocation(latitude: Double?, longitude: Double?): Result<Unit> {
        val current = MockDatabase.currentUser.value ?: return Result.failure(Exception("Not logged in"))
        
        val updatedUser = current.copy(
            latitude = latitude,
            longitude = longitude,
            locationTimestamp = System.currentTimeMillis()
        )
        MockDatabase.currentUser.value = updatedUser
        
        val updatedUsers = MockDatabase.users.value.toMutableMap()
        updatedUsers[current.uid] = updatedUser
        MockDatabase.users.value = updatedUsers
        
        return Result.success(Unit)
    }

    override fun observePartnerStatus(partnerUid: String): Flow<UserStatus?> {
        return MockDatabase.statuses.map { statusMap ->
            statusMap[partnerUid]
        }
    }

    override fun observePartnerLocation(partnerUid: String): Flow<Pair<Double, Double>?> {
        return MockDatabase.users.map { userMap ->
            val user = userMap[partnerUid]
            if (user?.latitude != null && user.longitude != null) {
                Pair(user.latitude, user.longitude)
            } else {
                null
            }
        }
    }

    override fun observePartnerUser(partnerUid: String): Flow<User?> {
        return MockDatabase.users.map { userMap ->
            userMap[partnerUid]
        }
    }

    override suspend fun getPartnerStatus(partnerUid: String): UserStatus? {
        return MockDatabase.statuses.value[partnerUid]
    }

    override suspend fun getPartnerUser(partnerUid: String): User? {
        return MockDatabase.users.value[partnerUid]
    }
}
