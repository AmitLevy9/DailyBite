package com.example.dailybite.data.post

data class CommentItem(
    val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)