package com.example.dailybite.data.user

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UserDoc(
    val uid: String = "",
    val name: String = "",
    val profileImagePath: String = ""
)

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private fun users() = firestore.collection("users")

    fun userFlow(uid: String): Flow<UserDoc?> = callbackFlow {
        val reg: ListenerRegistration = users().document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                if (snap != null && snap.exists()) {
                    val name = snap.getString("name") ?: ""
                    val path = snap.getString("profileImagePath") ?: ""
                    trySend(UserDoc(uid, name, path))
                } else trySend(null)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateProfile(uid: String, name: String, newImageUri: Uri?): Result<Unit> = runCatching {
        var pathToSave: String? = null
        if (newImageUri != null) {
            val path = "users/$uid/avatar.jpg"
            storage.reference.child(path).putFile(newImageUri).await()
            pathToSave = path
        }
        val data = mutableMapOf<String, Any>(
            "name" to name,
            "updatedAt" to System.currentTimeMillis()
        )
        if (pathToSave != null) data["profileImagePath"] = pathToSave
        users().document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun downloadUrlOrNull(path: String): String? =
        runCatching { storage.reference.child(path).downloadUrl.await().toString() }.getOrNull()
}