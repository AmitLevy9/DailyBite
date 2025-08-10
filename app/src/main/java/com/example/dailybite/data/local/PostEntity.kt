package com.example.dailybite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val mealType: String,
    val description: String,
    val imageStoragePath: String,
    val createdAt: Long,
    val likesCount: Int
)