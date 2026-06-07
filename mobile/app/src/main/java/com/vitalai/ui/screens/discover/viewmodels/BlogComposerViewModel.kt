package com.vitalai.ui.screens.discover.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.CreateBlogBlockRequest
import com.vitalai.data.remote.model.CreateBlogRequest
import com.vitalai.data.repository.BlogRepository
import com.vitalai.ui.screens.discover.components.mergeBlogTags
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
    val imageBase64: String = "",
    val localImageUri: String = "",
    val caption: String = ""
)

data class BlogComposerUiState(
    val editingBlogId: String? = null,
    val title: String = "",
    val coverUrl: String = "",
    val coverBase64: String = "",
    val localCoverUri: String = "",
    val tags: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val blocks: List<ContentBlock> = listOf(ContentBlock()),
    val tagInput: String = "",
    val isSaving: Boolean = false,
    val isPublishing: Boolean = false,
    val isLoadingBlog: Boolean = false,
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

    init {
        loadAvailableTags()
    }

    private fun loadAvailableTags() {
        viewModelScope.launch {
            blogRepository.getTags().onSuccess { tags ->
                _uiState.update {
                    it.copy(availableTags = mergeBlogTags(tags))
                }
            }
        }
    }

    fun loadBlogForEdit(blogId: String?) {
        if (blogId.isNullOrBlank()) return
        val state = _uiState.value
        if (state.editingBlogId == blogId && state.title.isNotBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(editingBlogId = blogId, isLoadingBlog = true, error = null) }
            blogRepository.getMyBlogById(blogId).fold(
                onSuccess = { blog ->
                    _uiState.update { blog.toComposerState() }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingBlog = false,
                            error = err.message ?: "Không tải được bài viết"
                        )
                    }
                }
            )
        }
    }

    fun setTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun setCoverUrl(url: String) = _uiState.update { it.copy(coverUrl = url, coverBase64 = "", localCoverUri = "") }
    fun setCoverImage(base64: String, localUri: String) = _uiState.update { it.copy(coverBase64 = base64, localCoverUri = localUri, coverUrl = "") }
    fun clearCoverImage() = _uiState.update { it.copy(coverUrl = "", coverBase64 = "", localCoverUri = "") }
    fun setTagInput(input: String) = _uiState.update { it.copy(tagInput = input) }

    fun addTag(tag: String) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isBlank() || _uiState.value.tags.any { it.equals(normalizedTag, ignoreCase = true) }) return
        _uiState.update { it.copy(tags = it.tags + normalizedTag, tagInput = "") }
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
            state.copy(blocks = state.blocks.map {
                if (it.id == id) it.copy(imageUrl = imageUrl, imageBase64 = "", localImageUri = "", caption = caption) else it
            })
        }
    }

    fun updateBlockLocalImage(id: String, base64: String, localUri: String) {
        _uiState.update { state ->
            state.copy(blocks = state.blocks.map {
                if (it.id == id) it.copy(imageBase64 = base64, localImageUri = localUri, imageUrl = "") else it
            })
        }
    }

    fun clearBlockImage(id: String) {
        _uiState.update { state ->
            state.copy(blocks = state.blocks.map {
                if (it.id == id) it.copy(imageUrl = "", imageBase64 = "", localImageUri = "") else it
            })
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
            val request = buildRequest(status = "draft") ?: return@launch
            _uiState.update { it.copy(isSaving = true, error = null, savedDraft = false) }
            val result = _uiState.value.editingBlogId?.let { id ->
                blogRepository.updateBlog(id, request)
            } ?: blogRepository.createBlog(request)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(isSaving = false, savedDraft = true, error = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Không lưu được bản nháp"
                        )
                    }
                }
            )
        }
    }

    fun publishPost() {
        viewModelScope.launch {
            val request = buildRequest(status = null) ?: return@launch
            _uiState.update { it.copy(isPublishing = true, error = null, published = false) }
            val result = _uiState.value.editingBlogId?.let { id ->
                blogRepository.updateBlog(id, request)
            } ?: blogRepository.createBlog(request)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(isPublishing = false, published = true, error = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            error = error.message ?: "Không đăng được bài viết"
                        )
                    }
                }
            )
        }
    }

    private fun buildRequest(status: String?): CreateBlogRequest? {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề bài viết") }
            return null
        }

        val blocks = state.blocks.mapIndexedNotNull { index, block ->
            when (block.type) {
                ContentBlockType.TEXT -> block.text.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        CreateBlogBlockRequest(
                            order = index,
                            type = "text",
                            textContent = it
                        )
                    }

                ContentBlockType.IMAGE -> block.imageUrl.trim()
                    .takeIf { it.isNotBlank() || block.imageBase64.isNotBlank() }
                    ?.let {
                        CreateBlogBlockRequest(
                            order = index,
                            type = "image",
                            imageBase64 = block.imageBase64.takeIf { base64 -> base64.isNotBlank() },
                            imageUrl = it.takeIf { url -> url.isNotBlank() }
                        )
                    } ?: block.imageBase64.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        CreateBlogBlockRequest(
                            order = index,
                            type = "image",
                            imageBase64 = it
                        )
                    }
            }
        }

        return CreateBlogRequest(
            title = title,
            thumbnailBase64 = state.coverBase64.trim().takeIf { it.isNotBlank() },
            thumbnailUrl = state.coverUrl.trim().takeIf { it.isNotBlank() },
            tags = state.tags.takeIf { it.isNotEmpty() },
            status = status,
            blocks = blocks.takeIf { it.isNotEmpty() }
        )
    }

    private fun BlogDto.toComposerState(): BlogComposerUiState {
        val mappedBlocks = blocks
            ?.sortedBy { it.order }
            ?.map { block ->
                when (block.type) {
                    "image" -> ContentBlock(
                        type = ContentBlockType.IMAGE,
                        imageUrl = block.imageUrl ?: ""
                    )
                    else -> ContentBlock(
                        type = ContentBlockType.TEXT,
                        text = block.textContent ?: ""
                    )
                }
            }
            ?.takeIf { it.isNotEmpty() }
            ?: content
                ?.takeIf { it.isNotBlank() }
                ?.let { listOf(ContentBlock(text = it)) }
            ?: listOf(ContentBlock())

        return BlogComposerUiState(
            editingBlogId = id,
            title = title,
            coverUrl = thumbnailUrl ?: "",
            tags = tags.orEmpty(),
            blocks = mappedBlocks,
            isLoadingBlog = false
        )
    }
}
