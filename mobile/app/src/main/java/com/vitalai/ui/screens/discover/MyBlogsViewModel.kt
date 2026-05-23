package com.vitalai.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.repository.BlogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyBlogsUiState(
    val allBlogs: List<BlogDto> = emptyList(),
    val statusFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredBlogs: List<BlogDto> get() =
        if (statusFilter == null) allBlogs else allBlogs.filter { it.status == statusFilter }

    val statusCounts: Map<String, Int> get() = mapOf(
        "approved" to allBlogs.count { it.status == "approved" },
        "pending" to allBlogs.count { it.status == "pending" },
        "draft" to allBlogs.count { it.status == "draft" },
        "rejected" to allBlogs.count { it.status == "rejected" }
    )

    val totalViews: Int get() = allBlogs.sumOf { it.viewCount }
    val totalLikes: Int get() = allBlogs.sumOf { it.likesCount }
    val approvedCount: Int get() = statusCounts["approved"] ?: 0
}

@HiltViewModel
class MyBlogsViewModel @Inject constructor(
    private val blogRepository: BlogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBlogsUiState())
    val uiState = _uiState.asStateFlow()

    init { loadMyBlogs() }

    fun loadMyBlogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            blogRepository.getBlogs(page = 1, limit = 50).fold(
                onSuccess = { page ->
                    _uiState.update { it.copy(allBlogs = page.items, isLoading = false) }
                },
                onFailure = { e ->
                    // Fall back to mock data
                    _uiState.update { it.copy(allBlogs = mockMyBlogs(), isLoading = false, error = null) }
                }
            )
        }
    }

    fun setStatusFilter(status: String?) = _uiState.update { it.copy(statusFilter = status) }

    fun deleteBlog(id: String) = _uiState.update { it.copy(allBlogs = it.allBlogs.filter { b -> b.id != id }) }

    private fun mockMyBlogs(): List<BlogDto> = listOf(
        BlogDto("1", "Hướng dẫn tập Bench Press đúng kỹ thuật", "Tôi", null, "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400", listOf("Tập luyện"), "approved", 45, 320, "2026-04-15"),
        BlogDto("2", "Chế độ ăn uống cho người tập gym", "Tôi", null, null, listOf("Dinh dưỡng"), "approved", 28, 180, "2026-04-20"),
        BlogDto("3", "Review phòng gym mới khai trương", "Tôi", null, null, listOf("Review"), "pending", 0, 0, "2026-05-01"),
        BlogDto("4", "Bài tập cardio tại nhà", "Tôi", null, null, listOf("Tập luyện"), "draft", 0, 0, "2026-05-10"),
        BlogDto("5", "Sự thật về thực phẩm chức năng", "Tôi", null, null, listOf("Dinh dưỡng"), "rejected", 0, 0, "2026-04-28")
    )
}
