package com.deep.sideglance.data

import android.content.Context
import android.util.Log
import com.deep.sideglance.data.repository.FirebaseAuthRepositoryImpl
import com.deep.sideglance.data.repository.FirebasePartnerRepositoryImpl
import com.deep.sideglance.data.repository.FirebaseStatusRepositoryImpl
import com.deep.sideglance.domain.repository.AuthRepository
import com.deep.sideglance.domain.repository.PartnerRepository
import com.deep.sideglance.domain.repository.StatusRepository
import com.google.firebase.FirebaseApp

object ServiceLocator {
    private const val TAG = "ServiceLocator"

    fun init(context: Context) {
        try {
            FirebaseApp.initializeApp(context)
            Log.d(TAG, "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed.", e)
        }
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepositoryImpl()
    }

    val statusRepository: StatusRepository by lazy {
        FirebaseStatusRepositoryImpl()
    }

    val partnerRepository: PartnerRepository by lazy {
        FirebasePartnerRepositoryImpl()
    }
}
