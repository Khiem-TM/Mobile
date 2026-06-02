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

data class AuthorBlogsUiState(
    val blogs: List<BlogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthorBlogsViewModel @Inject constructor(
    private val blogRepository: BlogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthorBlogsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadBlogs(authorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            blogRepository.getBlogs(authorId = authorId, limit = 50)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            blogs = page.items.sortedByDescending { blog -> blog.createdAt },
                            isLoading = false
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }
}
