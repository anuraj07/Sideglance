package com.deep.sideglance.data.repository

import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.repository.PartnerRepository

class MockPartnerRepositoryImpl : PartnerRepository {
    override suspend fun getPairingCode(): String {
        val current = MockDatabase.currentUser.value ?: throw Exception("Not logged in")
        return current.uid
    }

    override suspend fun pairWithPartner(code: String): Result<Unit> {
        val current = MockDatabase.currentUser.value ?: return Result.failure(Exception("Not logged in"))
        
        // Seed mock partner in DB
        MockDatabase.seedMockPartner(current.uid)
        
        val partnerUid = "mock_partner_123"
        val updatedCurrent = current.copy(partnerUid = partnerUid)
        MockDatabase.currentUser.value = updatedCurrent
        
        val updatedUsers = MockDatabase.users.value.toMutableMap()
        updatedUsers[current.uid] = updatedCurrent
        MockDatabase.users.value = updatedUsers
        
        return Result.success(Unit)
    }

    override suspend fun unpairPartner(): Result<Unit> {
        val current = MockDatabase.currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val partnerUid = current.partnerUid
        
        val updatedCurrent = current.copy(partnerUid = null)
        MockDatabase.currentUser.value = updatedCurrent
        
        val updatedUsers = MockDatabase.users.value.toMutableMap()
        updatedUsers[current.uid] = updatedCurrent
        
        if (partnerUid != null) {
            val partner = updatedUsers[partnerUid]
            if (partner != null) {
                updatedUsers[partnerUid] = partner.copy(partnerUid = null)
            }
        }
        MockDatabase.users.value = updatedUsers
        
        return Result.success(Unit)
    }
}
