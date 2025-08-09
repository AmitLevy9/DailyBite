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
    val loggedIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState(loading = false, loggedIn = repo.isLoggedIn())
    )
    val state: StateFlow<AuthUiState> = _state

    fun loginEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "נא למלא אימייל וסיסמה")
            return
        }
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val res = repo.signInEmail(email, password)
            _state.value = if (res.isSuccess) AuthUiState(loggedIn = true)
            else AuthUiState(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בהתחברות")
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _state.value = _state.value.copy(error = "סיסמה חייבת להיות באורך 6 תווים לפחות")
            return
        }
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val res = repo.signUpEmail(email, password)
            _state.value = if (res.isSuccess) AuthUiState(loggedIn = true)
            else AuthUiState(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בהרשמה")
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "נא למלא אימייל לאיפוס")
            return
        }
        viewModelScope.launch {
            val res = repo.sendPasswordReset(email)
            _state.value = if (res.isSuccess)
                _state.value.copy(error = "נשלח מייל לאיפוס סיסמה")
            else
                _state.value.copy(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בשליחת האימייל")
        }
    }

    fun loginGuest() {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val res = repo.signInAnonymously()
            _state.value = if (res.isSuccess) AuthUiState(loggedIn = true)
            else AuthUiState(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בכניסת אורח")
        }
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }
}