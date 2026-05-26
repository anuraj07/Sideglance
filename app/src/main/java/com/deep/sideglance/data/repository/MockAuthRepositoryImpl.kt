package com.deep.sideglance.data.repository

import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepositoryImpl : AuthRepository {
    override val currentUser: StateFlow<User?> = MockDatabase.currentUser.asStateFlow()

    override suspend fun signInWithGoogle(
        idToken: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<User> {
        val user = User(
            uid = "mock_user_999",
            email = email.ifEmpty { "user@example.com" },
            displayName = displayName.ifEmpty { "Self User" },
            photoUrl = photoUrl
        )
        // Store in Mock DB
        val updatedMap = MockDatabase.users.value.toMutableMap()
        updatedMap[user.uid] = user
        MockDatabase.users.value = updatedMap

        MockDatabase.currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        MockDatabase.currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun checkUserSession(): User? {
        return MockDatabase.currentUser.value
    }
}
