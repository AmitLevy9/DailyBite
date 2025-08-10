package com.example.dailybite.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val loading: Boolean = false,
    val name: String = "",
    val photoUrl: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state

    init {
        val uid = authRepo.currentUidOrNull()
        if (uid != null) {
            viewModelScope.launch {
                userRepo.userFlow(uid).collectLatest { doc ->
                    if (doc != null) {
                        val url = if (doc.profileImagePath.isNotEmpty())
                            userRepo.downloadUrlOrNull(doc.profileImagePath) else null
                        _state.value = _state.value.copy(name = doc.name, photoUrl = url)
                    }
                }
            }
        }
        // אם uid=null לא עושים כלום בינתיים
    }

    fun save(name: String, newImage: Uri?) {
        val uid = authRepo.currentUidOrNull() ?: return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val res = userRepo.updateProfile(uid, name, newImage)
            _state.value = if (res.isSuccess) {
                _state.value.copy(loading = false)
            } else {
                _state.value.copy(
                    loading = false,
                    error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בשמירה"
                )
            }
        }
    }

    fun consumeError() { _state.value = _state.value.copy(error = null) }
}