package com.vitalai.ui.screens.discover.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.CommentDto
import com.vitalai.data.repository.BlogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlogDetailUiState(
    val blog: BlogDto? = null,
    val comments: List<CommentDto> = emptyList(),
    val isLiked: Boolean = false,
    val isPostingComment: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class BlogDetailViewModel @Inject constructor(
    private val blogRepository: BlogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlogDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun loadBlog(id: String) {
        if (_uiState.value.blog?.id == id) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            blogRepository.getBlogById(id).onSuccess { blog ->
                _uiState.update { it.copy(blog = blog, isLoading = false) }
                loadEngagement(id)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Không tìm thấy bài viết") }
            }
        }
    }

    private fun loadEngagement(id: String) {
        viewModelScope.launch {
            blogRepository.isLiked(id).onSuccess { liked ->
                _uiState.update { it.copy(isLiked = liked) }
            }
            blogRepository.getComments(id).onSuccess { page ->
                _uiState.update { it.copy(comments = page.items) }
            }
        }
    }

    fun toggleLike() {
        val blog = _uiState.value.blog ?: return
        viewModelScope.launch {
            blogRepository.toggleLike(blog.id).onSuccess { liked ->
                val delta = if (liked) 1 else -1
                _uiState.update { state ->
                    state.copy(
                        isLiked = liked,
                        blog = state.blog?.copy(likesCount = (state.blog.likesCount + delta).coerceAtLeast(0))
                    )
                }
            }
        }
    }

    fun postComment(content: String) {
        val blog = _uiState.value.blog ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingComment = true) }
            blogRepository.postComment(blog.id, content.trim()).onSuccess { comment ->
                _uiState.update { state ->
                    state.copy(
                        isPostingComment = false,
                        comments = state.comments + comment,
                        blog = state.blog?.copy(commentCount = state.blog.commentCount + 1)
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isPostingComment = false, error = e.message) }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val blog = _uiState.value.blog ?: return
        viewModelScope.launch {
            blogRepository.deleteComment(blog.id, commentId).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        comments = state.comments.filterNot { it.id == commentId },
                        blog = state.blog?.copy(commentCount = (state.blog.commentCount - 1).coerceAtLeast(0))
                    )
                }
            }
        }
    }
}
