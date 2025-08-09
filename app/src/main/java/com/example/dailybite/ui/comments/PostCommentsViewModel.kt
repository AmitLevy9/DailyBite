package com.example.dailybite.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.post.CommentItem
import com.example.dailybite.data.post.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CommentsUiState(
    val items: List<CommentItem> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PostCommentsViewModel @Inject constructor(
    private val repo: PostRepository,
    private val auth: AuthRepository
) : ViewModel() {

    fun stream(postId: String): StateFlow<CommentsUiState> =
        repo.commentsFlow(postId)
            .map { CommentsUiState(items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommentsUiState())

    suspend fun send(postId: String, text: String): Result<Unit> {
        val uid = auth.currentUidOrNull() ?: return Result.failure(IllegalStateException("לא מחובר/ת"))
        return repo.addComment(postId, uid, text)
    }
}