package com.example.dailybite.data.post

data class PostItem(
    val id: String = "",
    val ownerUid: String = "",
    val mealType: String = "",
    val description: String = "",
    val imageStoragePath: String = "",
    val createdAt: Long = 0L,
    val likesCount: Int = 0
)