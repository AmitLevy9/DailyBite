package com.example.dailybite.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
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
        res.user?.uid ?: error("Email sign-up failed")
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        auth.signOut()
    }
}