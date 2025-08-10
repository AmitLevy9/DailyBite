package com.example.dailybite.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.post.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditPostState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val repo: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditPostState())
    val state: StateFlow<EditPostState> = _state


    fun save(
        postId: String,
        mealType: String,
        description: String,
        imageStoragePath: String,
        newImageBytes: ByteArray?
    ) {
        _state.value = EditPostState(loading = true)
        viewModelScope.launch {
            val res = repo.updatePost(postId, mealType, description, imageStoragePath, newImageBytes)
            _state.value = if (res.isSuccess) {
                EditPostState(success = true)
            } else {
                EditPostState(error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה בשמירת פוסט")
            }
        }
    }

    fun consumeSuccess() { _state.value = _state.value.copy(success = false) }
    fun consumeError() { _state.value = _state.value.copy(error = null) }

    fun delete(postId: String, imageStoragePath: String) {
        _state.value = _state.value.copy(loading = true, error = null, deleted = false)
        viewModelScope.launch {
            val res = repo.deletePost(postId, imageStoragePath)
            _state.value = if (res.isSuccess) {
                _state.value.copy(loading = false, deleted = true)
            } else {
                _state.value.copy(loading = false, error = res.exceptionOrNull()?.localizedMessage ?: "שגיאה במחיקה")
            }
        }
    }
}