package com.deep.sideglance.data.repository

import com.deep.sideglance.data.model.FirestoreStatus
import com.deep.sideglance.data.model.FirestoreUser
import com.deep.sideglance.domain.model.UserStatus
import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.repository.StatusRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseStatusRepositoryImpl : StatusRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun updateStatus(status: UserStatus): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        firestore.collection("statuses")
            .document(uid)
            .set(FirestoreStatus.fromDomain(status))
            .await()
    }

    override suspend fun updateLocation(latitude: Double?, longitude: Double?): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        firestore.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "locationTimestamp" to System.currentTimeMillis()
                )
            )
            .await()
    }

    override fun observePartnerStatus(partnerUid: String): Flow<UserStatus?> = callbackFlow {
        val listener = firestore.collection("statuses")
            .document(partnerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val firestoreStatus = snapshot?.toObject(FirestoreStatus::class.java)
                trySend(firestoreStatus?.toDomain())
            }
        awaitClose { listener.remove() }
    }

    override fun observePartnerLocation(partnerUid: String): Flow<Pair<Double, Double>?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(partnerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(FirestoreUser::class.java)
                if (user?.latitude != null && user.longitude != null) {
                    trySend(Pair(user.latitude, user.longitude))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observePartnerUser(partnerUid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(partnerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(FirestoreUser::class.java)
                trySend(user?.toDomain())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPartnerStatus(partnerUid: String): UserStatus? = runCatching {
        val snapshot = firestore.collection("statuses")
            .document(partnerUid)
            .get()
            .await()
        snapshot.toObject(FirestoreStatus::class.java)?.toDomain()
    }.getOrNull()

    override suspend fun getPartnerUser(partnerUid: String): User? = runCatching {
        val snapshot = firestore.collection("users")
            .document(partnerUid)
            .get()
            .await()
        snapshot.toObject(FirestoreUser::class.java)?.toDomain()
    }.getOrNull()
}
