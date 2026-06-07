package com.vitalai.ui.screens.discover.components

val SystemBlogTags = listOf(
    "Dinh dưỡng",
    "Tập luyện",
    "Sức khỏe",
    "Giảm cân",
    "Eat clean"
)

fun mergeBlogTags(tags: List<String>): List<String> {
    return (SystemBlogTags + tags)
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
}
