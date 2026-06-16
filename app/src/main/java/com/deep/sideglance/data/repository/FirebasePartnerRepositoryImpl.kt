package com.deep.sideglance.data.repository

import com.deep.sideglance.domain.repository.PartnerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebasePartnerRepositoryImpl : PartnerRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun getPairingCode(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    override suspend fun pairWithPartner(code: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        if (uid == code) {
            throw Exception("You cannot pair with yourself")
        }

        // Verify the partner code corresponds to an existing user
        val partnerDoc = firestore.collection("sideglance/app/users").document(code).get().await()
        if (!partnerDoc.exists()) {
            throw Exception("Invalid pairing code: user not found")
        }

        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("sideglance/app/users").document(uid)
            val partnerRef = firestore.collection("sideglance/app/users").document(code)

            transaction.update(userRef, "partnerUid", code)
            transaction.update(partnerRef, "partnerUid", uid)
        }.await()
    }

    override suspend fun unpairPartner(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val userDoc = firestore.collection("sideglance/app/users").document(uid).get().await()
        val partnerUid = userDoc.getString("partnerUid")

        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("sideglance/app/users").document(uid)
            transaction.update(userRef, "partnerUid", null)

            if (partnerUid != null) {
                val partnerRef = firestore.collection("sideglance/app/users").document(partnerUid)
                transaction.update(partnerRef, "partnerUid", null)
            }
        }.await()
    }
}
