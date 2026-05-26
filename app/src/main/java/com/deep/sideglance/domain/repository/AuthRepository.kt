package com.deep.sideglance.domain.repository

import com.deep.sideglance.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun signInWithGoogle(idToken: String, displayName: String, email: String, photoUrl: String?): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun checkUserSession(): User?
}
