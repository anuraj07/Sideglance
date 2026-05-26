package com.deep.sideglance.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val partnerUid: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationTimestamp: Long? = null
)
