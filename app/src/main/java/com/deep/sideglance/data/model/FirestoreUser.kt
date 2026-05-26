package com.deep.sideglance.data.model

import com.deep.sideglance.domain.model.User

data class FirestoreUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val partnerUid: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationTimestamp: Long? = null
) {
    fun toDomain(): User = User(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        partnerUid = partnerUid,
        latitude = latitude,
        longitude = longitude,
        locationTimestamp = locationTimestamp
    )

    companion object {
        fun fromDomain(user: User): FirestoreUser = FirestoreUser(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            partnerUid = user.partnerUid,
            latitude = user.latitude,
            longitude = user.longitude,
            locationTimestamp = user.locationTimestamp
        )
    }
}
