package com.example.dailybite.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun checkLoggedIn() {
        if (repo.isLoggedIn()) {
            _state.value = AuthUiState(loggedIn = true)
        }
    }

    fun signInAnon() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val res = repo.signInAnonymously()
            _state.value = if (res.isSuccess) {
                AuthUiState(loggedIn = true)
            } else {
                AuthUiState(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בכניסה")
            }
        }
    }
}