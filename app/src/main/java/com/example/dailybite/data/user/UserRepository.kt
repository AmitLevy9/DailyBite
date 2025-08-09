package com.example.dailybite.data.user

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val displayName: String,
    val photoUrl: String?
)

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    fun currentUserProfileFlow(uid: String) = callbackFlow<UserProfile?> {
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null).isSuccess
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    val displayName = snap.getString("displayName") ?: ""
                    val photoPath = snap.getString("photoPath")
                    val photoUrl = photoPath?.let { storage.reference.child(it).path }
                    trySend(UserProfile(displayName, photoUrl)).isSuccess
                } else {
                    trySend(null).isSuccess
                }
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateProfile(uid: String, name: String, imageUri: Uri?): Result<Unit> = runCatching {
        var photoPath: String? = null
        if (imageUri != null) {
            photoPath = "users/$uid/avatar.jpg"
            storage.reference.child(photoPath).putFile(imageUri).await()
        }
        val updateData = mutableMapOf<String, Any>(
            "displayName" to name,
            "updatedAt" to System.currentTimeMillis()
        )
        if (photoPath != null) updateData["photoPath"] = photoPath
        firestore.collection("users").document(uid).update(updateData).await()
    }
}