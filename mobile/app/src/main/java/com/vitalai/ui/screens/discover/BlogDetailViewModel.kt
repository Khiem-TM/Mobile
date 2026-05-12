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

data class BlogDetailUiState(
    val blog: BlogDto? = null,
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
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Không tìm thấy bài viết") }
            }
        }
    }
}
