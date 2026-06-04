package com.vitalai.ui.screens.discover.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.local.BlogSearchHistoryStore
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.repository.BlogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class BlogSearchSortOption(val label: String) {
    LATEST("Mới nhất"),
    OLDEST("Cũ nhất")
}

enum class BlogSearchDateFilter(val label: String, val days: Long) {
    ONE_DAY("1 ngày trước", 1),
    ONE_WEEK("1 tuần trước", 7),
    ONE_MONTH("1 tháng trước", 30),
    SIX_MONTHS("6 tháng trước", 183),
    ONE_YEAR("1 năm trước", 365)
}

data class BlogSearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val sortOption: BlogSearchSortOption = BlogSearchSortOption.LATEST,
    val dateFilter: BlogSearchDateFilter? = null,
    val selectedTag: String? = null,
    val history: List<String> = emptyList(),
    val blogs: List<BlogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val results: List<BlogDto>
        get() {
            val q = submittedQuery.trim()
            if (q.isBlank()) return emptyList()
            return blogs.filter { blog ->
                blog.title.contains(q, ignoreCase = true) ||
                    blog.displayAuthor.contains(q, ignoreCase = true) ||
                    blog.tags.orEmpty().any { it.contains(q, ignoreCase = true) } ||
                    blog.content.orEmpty().contains(q, ignoreCase = true) ||
                blog.blocks.orEmpty().any { it.textContent.orEmpty().contains(q, ignoreCase = true) }
            }
                .let { items ->
                    dateFilter?.let { filter ->
                        val minDate = LocalDate.now().minusDays(filter.days)
                        items.filter { blog ->
                            blog.createdLocalDateOrNull()?.let { !it.isBefore(minDate) } ?: false
                        }
                    } ?: items
                }
                .let { items ->
                    selectedTag?.let { tag ->
                        items.filter { blog -> blog.tags.orEmpty().any { it.equals(tag, ignoreCase = true) } }
                    } ?: items
                }
                .let { items ->
                    when (sortOption) {
                        BlogSearchSortOption.LATEST -> items.sortedByDescending { it.createdAt }
                        BlogSearchSortOption.OLDEST -> items.sortedBy { it.createdAt }
                    }
                }
        }

    val activeFilterCount: Int
        get() = listOfNotNull(dateFilter, selectedTag).size

    val availableTags: List<String>
        get() = blogs
            .flatMap { it.tags.orEmpty() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

    val popularSearches: List<String>
        get() {
            val tags = blogs
                .flatMap { it.tags.orEmpty() }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }

            val titles = blogs
                .map { it.title.trim() }
                .filter { it.isNotBlank() }

            return (tags + titles).distinctBy { it.lowercase() }.take(8)
        }

    val suggestions: List<String>
        get() {
            val q = query.trim()
            if (q.isBlank()) return popularSearches

            val tags = blogs
                .flatMap { it.tags.orEmpty() }
                .map { it.trim() }
                .filter { it.contains(q, ignoreCase = true) }

            val titles = blogs
                .map { it.title.trim() }
                .filter { it.contains(q, ignoreCase = true) }

            val authors = blogs
                .map { it.displayAuthor.trim() }
                .filter { it.contains(q, ignoreCase = true) }

            return (tags + titles + authors)
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(8)
        }
}

@HiltViewModel
class BlogSearchViewModel @Inject constructor(
    private val blogRepository: BlogRepository,
    private val searchHistoryStore: BlogSearchHistoryStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(BlogSearchUiState())
    val uiState = _uiState.asStateFlow()
    private var autoSaveSearchJob: Job? = null

    init {
        observeHistory()
        loadBlogs()
    }

    fun setQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                submittedQuery = if (query.trim() == it.submittedQuery.trim()) it.submittedQuery else ""
            )
        }
        scheduleSearchHistorySave(query)
    }

    fun submitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        _uiState.update { it.copy(query = query, submittedQuery = query) }
        viewModelScope.launch {
            searchHistoryStore.save(query)
        }
    }

    fun selectHistory(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return
        _uiState.update { it.copy(query = normalizedQuery, submittedQuery = normalizedQuery) }
        viewModelScope.launch {
            searchHistoryStore.save(normalizedQuery)
        }
    }

    fun clearQuery() {
        autoSaveSearchJob?.cancel()
        _uiState.update { it.copy(query = "", submittedQuery = "") }
    }

    fun setSortOption(option: BlogSearchSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setDateFilter(filter: BlogSearchDateFilter?) {
        _uiState.update { it.copy(dateFilter = filter) }
    }

    fun setTagFilter(tag: String?) {
        _uiState.update { it.copy(selectedTag = tag) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(dateFilter = null, selectedTag = null) }
    }

    fun removeHistory(query: String) {
        viewModelScope.launch {
            searchHistoryStore.remove(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryStore.clear()
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            searchHistoryStore.history.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    private fun scheduleSearchHistorySave(query: String) {
        autoSaveSearchJob?.cancel()
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return

        autoSaveSearchJob = viewModelScope.launch {
            delay(900)
            if (_uiState.value.query.trim() == normalizedQuery) {
                searchHistoryStore.save(normalizedQuery)
            }
        }
    }

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

private fun BlogDto.createdLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(createdAt.take(10)) }.getOrNull()
}
