package com.example.dailybite.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null
    fun currentUidOrNull(): String? = firebaseAuth.currentUser?.uid

    suspend fun signInAnonymously(): Result<Unit> = runCatching {
        firebaseAuth.signInAnonymously().await()
        Unit
    }

    // הרחבה בהמשך: אימייל+סיסמה
    // suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching { ... }
}