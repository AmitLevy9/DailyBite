package com.example.dailybite.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailybite.data.post.PostItem
import com.example.dailybite.data.post.PostRepository
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FeedUiState(
    val loading: Boolean = true,
    val items: List<PostItem> = emptyList()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repo: PostRepository
) : ViewModel() {

    private var syncReg: ListenerRegistration? = null

    val state: StateFlow<FeedUiState> =
        repo.feedLocalFlow()
            .map { FeedUiState(loading = false, items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    init {
        // מפעיל סנכרון מרחוק -> מקומי
        syncReg = repo.startFeedSync()
    }

    override fun onCleared() {
        super.onCleared()
        syncReg?.remove()
    }
}