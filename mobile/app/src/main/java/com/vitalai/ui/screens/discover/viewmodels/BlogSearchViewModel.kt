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

data class BlogSearchUiState(
    val query: String = "",
    val blogs: List<BlogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val results: List<BlogDto>
        get() {
            val q = query.trim()
            if (q.isBlank()) return emptyList()
            return blogs.filter { blog ->
                blog.title.contains(q, ignoreCase = true) ||
                    blog.displayAuthor.contains(q, ignoreCase = true) ||
                    blog.tags.orEmpty().any { it.contains(q, ignoreCase = true) } ||
                    blog.content.orEmpty().contains(q, ignoreCase = true) ||
                    blog.blocks.orEmpty().any { it.textContent.orEmpty().contains(q, ignoreCase = true) }
            }
        }
}

@HiltViewModel
class BlogSearchViewModel @Inject constructor(
    private val blogRepository: BlogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BlogSearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadBlogs()
    }

    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun loadBlogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            blogRepository.getBlogs(limit = 50).fold(
                onSuccess = { page -> _uiState.update { it.copy(blogs = page.items, isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }
}
