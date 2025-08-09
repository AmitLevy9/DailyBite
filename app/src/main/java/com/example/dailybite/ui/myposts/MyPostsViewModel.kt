package com.example.dailybite.ui.myposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.post.PostItem
import com.example.dailybite.data.post.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MyPostsUiState(
    val loading: Boolean = true,
    val items: List<PostItem> = emptyList()
)

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    auth: AuthRepository,
    repo: PostRepository
) : ViewModel() {

    private val uid = auth.currentUidOrNull()

    val state: StateFlow<MyPostsUiState> =
        (uid?.let { repo.myPostsFlow(it) } ?: flowOf(emptyList()))
            .map { MyPostsUiState(loading = false, items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyPostsUiState())
}