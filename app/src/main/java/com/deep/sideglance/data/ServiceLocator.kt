package com.deep.sideglance.data

import android.content.Context
import android.util.Log
import com.deep.sideglance.data.repository.*
import com.deep.sideglance.domain.repository.AuthRepository
import com.deep.sideglance.domain.repository.PartnerRepository
import com.deep.sideglance.domain.repository.StatusRepository
import com.google.firebase.FirebaseApp

object ServiceLocator {
    private const val TAG = "ServiceLocator"
    
    private var isFirebaseInitialized = false

    fun init(context: Context) {
        isFirebaseInitialized = try {
            FirebaseApp.initializeApp(context)
            Log.d(TAG, "Firebase initialized successfully.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization failed. Falling back to Mock Mode.", e)
            false
        }
    }

    val authRepository: AuthRepository by lazy {
        if (isFirebaseInitialized) {
            FirebaseAuthRepositoryImpl()
        } else {
            MockAuthRepositoryImpl()
        }
    }

    val statusRepository: StatusRepository by lazy {
        if (isFirebaseInitialized) {
            FirebaseStatusRepositoryImpl()
        } else {
            MockStatusRepositoryImpl()
        }
    }

    val partnerRepository: PartnerRepository by lazy {
        if (isFirebaseInitialized) {
            FirebasePartnerRepositoryImpl()
        } else {
            MockPartnerRepositoryImpl()
        }
    }
}
