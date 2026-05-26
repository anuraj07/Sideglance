package com.deep.sideglance

import com.deep.sideglance.data.repository.*
import com.deep.sideglance.domain.model.UserStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockRepositoriesTest {

    private lateinit var authRepository: MockAuthRepositoryImpl
    private lateinit var statusRepository: MockStatusRepositoryImpl
    private lateinit var partnerRepository: MockPartnerRepositoryImpl

    @Before
    fun setUp() {
        MockDatabase.reset()
        authRepository = MockAuthRepositoryImpl()
        statusRepository = MockStatusRepositoryImpl()
        partnerRepository = MockPartnerRepositoryImpl()
    }

    @Test
    fun testGoogleSignInAndSignOut() = runBlocking {
        // Sign in
        val result = authRepository.signInWithGoogle(
            idToken = "mock_token",
            displayName = "Alex",
            email = "alex@example.com",
            photoUrl = null
        )
        assertTrue(result.isSuccess)
        
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("mock_user_999", user?.uid)
        assertEquals("Alex", user?.displayName)
        assertEquals("alex@example.com", user?.email)
        
        // Check session
        val sessionUser = authRepository.checkUserSession()
        assertNotNull(sessionUser)
        assertEquals("Alex", sessionUser?.displayName)
        
        // Sign out
        val signOutResult = authRepository.signOut()
        assertTrue(signOutResult.isSuccess)
        assertNull(authRepository.currentUser.value)
    }

    @Test
    fun testStatusUpdateAndExpiration() = runBlocking {
        // Sign in first
        authRepository.signInWithGoogle("token", "Alex", "alex@example.com", null)
        
        val status = UserStatus(
            statusType = "💼 Work",
            detail = "Coding tests",
            timestamp = System.currentTimeMillis(),
            expirationTime = System.currentTimeMillis() + 1000 * 60 // Expires in 1 min
        )
        
        val result = statusRepository.updateStatus(status)
        assertTrue(result.isSuccess)
        
        // Verify expiration logic
        assertFalse(status.isExpired)
        
        // Make an expired status
        val expiredStatus = UserStatus(
            statusType = "🍳 Cook",
            detail = "Pasta",
            timestamp = System.currentTimeMillis() - 5000,
            expirationTime = System.currentTimeMillis() - 1000 // Expired 1 sec ago
        )
        assertTrue(expiredStatus.isExpired)
    }

    @Test
    fun testPartnerPairingFlow() = runBlocking {
        // Sign in
        authRepository.signInWithGoogle("token", "Alex", "alex@example.com", null)
        
        val code = partnerRepository.getPairingCode()
        assertEquals("mock_user_999", code)
        
        // Pair with partner
        val pairResult = partnerRepository.pairWithPartner("mock_partner_123")
        assertTrue(pairResult.isSuccess)
        
        // Verify current user has partner set
        val currentUser = authRepository.currentUser.value
        assertEquals("mock_partner_123", currentUser?.partnerUid)
        
        // Verify partner status can be observed
        val observedStatus = statusRepository.observePartnerStatus("mock_partner_123").first()
        assertNotNull(observedStatus)
        assertEquals("Cook", observedStatus?.statusType)
        assertEquals("Baking chocolate chip cookies 🍪", observedStatus?.detail)
        
        // Unpair
        val unpairResult = partnerRepository.unpairPartner()
        assertTrue(unpairResult.isSuccess)
        assertNull(authRepository.currentUser.value?.partnerUid)
    }
}
