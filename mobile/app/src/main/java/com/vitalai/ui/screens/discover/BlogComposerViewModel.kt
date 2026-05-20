package com.vitalai.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.repository.BlogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ContentBlockType { TEXT, IMAGE }

data class ContentBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: ContentBlockType = ContentBlockType.TEXT,
    val text: String = "",
    val imageUrl: String = "",
    val caption: String = ""
)

data class BlogComposerUiState(
    val title: String = "",
    val coverUrl: String = "",
    val tags: List<String> = emptyList(),
    val blocks: List<ContentBlock> = listOf(ContentBlock()),
    val tagInput: String = "",
    val isSaving: Boolean = false,
    val isPublishing: Boolean = false,
    val savedDraft: Boolean = false,
    val published: Boolean = false,
    val error: String? = null,
    val showAddBlockSheet: Boolean = false
)

@HiltViewModel
class BlogComposerViewModel @Inject constructor(
    private val blogRepository: BlogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlogComposerUiState())
    val uiState = _uiState.asStateFlow()

    fun setTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun setCoverUrl(url: String) = _uiState.update { it.copy(coverUrl = url) }
    fun setTagInput(input: String) = _uiState.update { it.copy(tagInput = input) }

    fun addTag(tag: String) {
        if (tag.isBlank() || tag in _uiState.value.tags) return
        _uiState.update { it.copy(tags = it.tags + tag, tagInput = "") }
    }

    fun removeTag(tag: String) = _uiState.update { it.copy(tags = it.tags - tag) }

    fun addBlock(type: ContentBlockType) {
        _uiState.update { it.copy(blocks = it.blocks + ContentBlock(type = type), showAddBlockSheet = false) }
    }

    fun removeBlock(id: String) = _uiState.update { it.copy(blocks = it.blocks.filter { b -> b.id != id }) }

    fun updateBlockText(id: String, text: String) {
        _uiState.update { state ->
            state.copy(blocks = state.blocks.map { if (it.id == id) it.copy(text = text) else it })
        }
    }

    fun updateBlockImage(id: String, imageUrl: String, caption: String) {
        _uiState.update { state ->
            state.copy(blocks = state.blocks.map { if (it.id == id) it.copy(imageUrl = imageUrl, caption = caption) else it })
        }
    }

    fun reorderBlock(id: String, direction: Int) {
        _uiState.update { state ->
            val list = state.blocks.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            val newIdx = (idx + direction).coerceIn(0, list.size - 1)
            if (idx != newIdx) {
                val item = list.removeAt(idx)
                list.add(newIdx, item)
            }
            state.copy(blocks = list)
        }
    }

    fun setShowAddBlockSheet(show: Boolean) = _uiState.update { it.copy(showAddBlockSheet = show) }

    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            // Simulated save — in real app would POST to API
            kotlinx.coroutines.delay(800)
            _uiState.update { it.copy(isSaving = false, savedDraft = true) }
        }
    }

    fun publishPost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true) }
            // Simulated publish
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(isPublishing = false, published = true) }
        }
    }
}
