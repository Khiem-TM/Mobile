package com.vitalai.ui.screens.discover.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.CommentDto
import com.vitalai.data.repository.BlogRepository
import com.vitalai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlogDetailUiState(
    val blog: BlogDto? = null,
    val comments: List<CommentDto> = emptyList(),
    val currentUserId: String? = null,
    val isLiked: Boolean = false,
    val isUpdatingLike: Boolean = false,
    val likeError: String? = null,
    val isPostingComment: Boolean = false,
    val isUpdatingComment: Boolean = false,
    val commentActionError: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class BlogDetailViewModel @Inject constructor(
    private val blogRepository: BlogRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlogDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUserId = user?.id) }
            }
        }
    }

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
                _uiState.update {
                    if (it.isUpdatingLike) it else it.copy(isLiked = liked)
                }
            }
            blogRepository.getComments(id).onSuccess { page ->
                _uiState.update { it.copy(comments = page.items) }
            }
        }
    }

    fun toggleLike() {
        val currentState = _uiState.value
        val blog = currentState.blog ?: return
        if (currentState.isUpdatingLike) return

        val previousLiked = currentState.isLiked
        val previousLikesCount = blog.likesCount
        val optimisticLiked = !previousLiked
        val optimisticLikesCount = (
            previousLikesCount + if (optimisticLiked) 1 else -1
        ).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                isLiked = optimisticLiked,
                isUpdatingLike = true,
                likeError = null,
                blog = it.blog?.copy(likesCount = optimisticLikesCount)
            )
        }

        viewModelScope.launch {
            blogRepository.toggleLike(blog.id).onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        isLiked = result.liked,
                        isUpdatingLike = false,
                        blog = state.blog?.copy(likesCount = result.likesCount ?: optimisticLikesCount)
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isLiked = previousLiked,
                        isUpdatingLike = false,
                        likeError = "Không thể cập nhật lượt thích",
                        blog = state.blog?.copy(likesCount = previousLikesCount)
                    )
                }
            }
        }
    }

    fun clearLikeError() {
        _uiState.update { it.copy(likeError = null) }
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

    fun updateComment(commentId: String, content: String) {
        val state = _uiState.value
        val blog = state.blog ?: return
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank() || state.isUpdatingComment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingComment = true, commentActionError = null) }
            blogRepository.updateComment(blog.id, commentId, trimmedContent).onSuccess { updatedComment ->
                _uiState.update { current ->
                    current.copy(
                        isUpdatingComment = false,
                        comments = current.comments.map {
                            if (it.id == updatedComment.id) updatedComment else it
                        }
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isUpdatingComment = false,
                        commentActionError = "Không thể sửa bình luận"
                    )
                }
            }
        }
    }

    fun clearCommentActionError() {
        _uiState.update { it.copy(commentActionError = null) }
    }
}
