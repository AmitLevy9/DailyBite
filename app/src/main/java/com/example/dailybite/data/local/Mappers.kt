package com.example.dailybite.data.local

import com.example.dailybite.data.post.PostItem

fun PostItem.toEntity() = PostEntity(
    id = id,
    ownerUid = ownerUid,
    mealType = mealType,
    description = description,
    imageStoragePath = imageStoragePath,
    createdAt = createdAt,
    likesCount = likesCount
)

fun PostEntity.toItem() = PostItem(
    id = id,
    ownerUid = ownerUid,
    mealType = mealType,
    description = description,
    imageStoragePath = imageStoragePath,
    createdAt = createdAt,
    likesCount = likesCount
)