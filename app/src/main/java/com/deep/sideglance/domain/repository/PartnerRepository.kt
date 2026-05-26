package com.deep.sideglance.domain.repository

interface PartnerRepository {
    suspend fun getPairingCode(): String
    suspend fun pairWithPartner(code: String): Result<Unit>
    suspend fun unpairPartner(): Result<Unit>
}
