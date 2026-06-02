package com.vitalai.ui.screens.discover.components

import com.vitalai.data.remote.model.AuthorUserDto
import com.vitalai.data.remote.model.BlogDto

internal fun previewBlogDto() = BlogDto(
    id = "preview-blog",
    title = "7 nguyên tắc eat clean giúp giảm mỡ hiệu quả",
    author = "VitalAI",
    content = "Bài viết mẫu cho preview.",
    thumbnailUrl = null,
    tags = listOf("Dinh dưỡng", "Eat clean"),
    status = "approved",
    likesCount = 24,
    viewCount = 128,
    commentCount = 6,
    createdAt = "2026-06-01T10:00:00",
    authorUser = AuthorUserDto(
        id = "author-preview",
        displayName = "VitalAI",
        email = "vitalai@example.com",
        avatarUrl = null
    ),
    blocks = null
)
