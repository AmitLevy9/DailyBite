package com.example.dailybite.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.post.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewPostState(
    val loading: Boolean = false,
    val successId: String? = null,
    val error: String? = null
)

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val posts: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewPostState())
    val state: StateFlow<NewPostState> = _state

    fun publish(mealType: String, description: String, imageBytes: ByteArray) {
        val uid = auth.currentUidOrNull() ?: run {
            _state.value = NewPostState(error = "לא מחובר/ת")
            return
        }
        _state.value = NewPostState(loading = true)
        viewModelScope.launch {
            val res = posts.createPost(uid, mealType, description, imageBytes)
            _state.value = if (res.isSuccess) {
                NewPostState(successId = res.getOrNull())
            } else {
                NewPostState(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בעלאת פוסט")
            }
        }
    }
}