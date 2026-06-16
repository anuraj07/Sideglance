package com.deep.sideglance.data.repository

import com.deep.sideglance.data.model.FirestoreUser
import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl : AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                _currentUser.value = null
            } else {
                // Fetch user data from firestore
                observeCurrentUserProfile(firebaseUser.uid)
            }
        }
    }

    private fun observeCurrentUserProfile(uid: String) {
        firestore.collection("sideglance/app/users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val firestoreUser = snapshot.toObject(FirestoreUser::class.java)
                _currentUser.value = firestoreUser?.toDomain()
            }
    }

    override suspend fun signInWithGoogle(
        idToken: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user ?: throw Exception("Google Sign In failed: Empty Firebase User")
        
        // Save profile to firestore
        val userDoc = firestore.collection("sideglance/app/users").document(firebaseUser.uid)
        val existingDoc = userDoc.get().await()
        
        val user = if (existingDoc.exists()) {
            existingDoc.toObject(FirestoreUser::class.java)!!.toDomain()
        } else {
            val newUser = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                displayName = firebaseUser.displayName ?: displayName,
                photoUrl = firebaseUser.photoUrl?.toString() ?: photoUrl
            )
            userDoc.set(FirestoreUser.fromDomain(newUser)).await()
            newUser
        }
        _currentUser.value = user
        user
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
        _currentUser.value = null
    }

    override suspend fun checkUserSession(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("sideglance/app/users").document(firebaseUser.uid).get().await()
            val user = doc.toObject(FirestoreUser::class.java)?.toDomain()
            _currentUser.value = user
            user
        } catch (e: Exception) {
            null
        }
    }
}
