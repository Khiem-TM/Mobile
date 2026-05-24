package com.vitalai.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.repository.BlogRepository
import com.vitalai.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val blogs: List<BlogDto> = emptyList(),
    val exploreFoods: List<FoodDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val blogRepository: BlogRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTags()
        loadExploreFoods()
        loadBlogs()
    }

    fun loadExploreFoods() {
        viewModelScope.launch {
            foodRepository.exploreFoods(limit = 10).onSuccess { page ->
                _uiState.update { it.copy(exploreFoods = page.items) }
            }
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            blogRepository.getTags().onSuccess { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
    }

    fun loadBlogs() {
        val tag = _uiState.value.selectedTag
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            blogRepository.getBlogs(tag).onSuccess { page ->
                _uiState.update { it.copy(blogs = page.items, isLoading = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun filterByTag(tag: String?) {
        _uiState.update { it.copy(selectedTag = tag) }
        loadBlogs()
    }
}
