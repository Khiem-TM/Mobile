package com.vitalai.ui.screens.discover.viewmodels

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
            blogRepository.getMyBlogs(page = 1, limit = 50).fold(
                onSuccess = { page ->
                    _uiState.update { it.copy(allBlogs = page.items, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            allBlogs = emptyList(),
                            isLoading = false,
                            error = e.message ?: "Không tải được bài viết"
                        )
                    }
                }
            )
        }
    }

    fun setStatusFilter(status: String?) = _uiState.update { it.copy(statusFilter = status) }

    fun deleteBlog(id: String) {
        viewModelScope.launch {
            blogRepository.deleteMyBlog(id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(allBlogs = state.allBlogs.filter { blog -> blog.id != id })
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Không xóa được bài viết")
                    }
                }
            )
        }
    }
}
