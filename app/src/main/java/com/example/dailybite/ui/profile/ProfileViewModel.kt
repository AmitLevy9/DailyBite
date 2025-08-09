package com.example.dailybite.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    val profile = userRepo.currentUserProfileFlow(
        authRepo.currentUidOrNull() ?: ""
    ).stateIn(viewModelScope, SharingStarted.Lazily, null)

    suspend fun updateProfile(name: String, imageUri: Uri?): Result<Unit> {
        val uid = authRepo.currentUidOrNull() ?: return Result.failure(Exception("No user"))
        return userRepo.updateProfile(uid, name, imageUri)
    }
}