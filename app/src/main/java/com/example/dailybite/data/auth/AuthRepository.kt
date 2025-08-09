package com.example.dailybite.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore // נוסיף את Firestore כאן
) {
    fun currentUidOrNull(): String? = auth.currentUser?.uid
    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signInAnonymously(): Result<String> = runCatching {
        val res = auth.signInAnonymously().await()
        res.user?.uid ?: error("Anonymous sign-in failed")
    }

    suspend fun signInEmail(email: String, password: String): Result<String> = runCatching {
        val res = auth.signInWithEmailAndPassword(email.trim(), password).await()
        res.user?.uid ?: error("Email sign-in failed")
    }

    suspend fun signUpEmail(email: String, password: String): Result<String> = runCatching {
        val res = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = res.user?.uid ?: error("Email sign-up failed")
        createUserProfile(uid, res.user?.displayName) // ניצור את פרופיל המשתמש כאן
        uid
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun createUserProfile(uid: String, displayName: String?) {
        val userDoc = mapOf(
            "uid" to uid,
            "name" to (displayName ?: ""),
            "profileImagePath" to "", // ברירת מחדל (אין תמונה)
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).set(userDoc).await()
    }

    fun signOut() {
        auth.signOut()
    }
}