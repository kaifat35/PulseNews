package com.pulsenews.domain.entity

data class Article(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceName: String,
    val author: String,
    val publishedAt: Long,
    val url: String
)
