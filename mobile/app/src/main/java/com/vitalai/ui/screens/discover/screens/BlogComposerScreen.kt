package com.vitalai.ui.screens.discover.screens

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.screens.discover.viewmodels.BlogComposerUiState
import com.vitalai.ui.screens.discover.viewmodels.BlogComposerViewModel
import com.vitalai.ui.screens.discover.viewmodels.ContentBlock
import com.vitalai.ui.screens.discover.viewmodels.ContentBlockType
import com.vitalai.ui.theme.*

private sealed class BlogImageTarget {
    data object Cover : BlogImageTarget()
    data class Block(val blockId: String) : BlogImageTarget()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogComposerScreen(
    navController: NavController,
    blogId: String? = null,
    viewModel: BlogComposerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val isEditing = uiState.editingBlogId != null
    var titleFieldValue by remember { mutableStateOf(TextFieldValue(uiState.title)) }
    var imageTarget by remember { mutableStateOf<BlogImageTarget?>(null) }
    var urlTarget by remember { mutableStateOf<BlogImageTarget?>(null) }
    var urlInput by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    val canPreview = titleFieldValue.text.isNotBlank() && uiState.hasPreviewContent()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = imageTarget
        if (uri != null && target != null) {
            uri.toBase64DataUri(context)?.let { base64 ->
                when (target) {
                    BlogImageTarget.Cover -> viewModel.setCoverImage(base64, uri.toString())
                    is BlogImageTarget.Block -> viewModel.updateBlockLocalImage(target.blockId, base64, uri.toString())
                }
            }
        }
        imageTarget = null
    }

    LaunchedEffect(blogId) {
        viewModel.loadBlogForEdit(blogId)
    }

    LaunchedEffect(uiState.editingBlogId, uiState.isLoadingBlog) {
        if (!uiState.isLoadingBlog && uiState.editingBlogId != null && uiState.title != titleFieldValue.text) {
            titleFieldValue = TextFieldValue(uiState.title)
        }
    }

    // Navigate away after publish
    LaunchedEffect(uiState.published) {
        if (uiState.published) navController.popBackStack()
    }

    Scaffold(
        containerColor = AppMutedBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isEditing) "Sửa bài viết" else "Viết bài", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink900)
                        Text(if (isEditing) "Cập nhật bài cộng đồng" else "Chia sẻ kiến thức sức khỏe", fontSize = 12.sp, color = Ink500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .clickable(enabled = canPreview, onClick = {
                                viewModel.setTitle(titleFieldValue.text)
                                showPreview = true
                            }),
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = if (canPreview) Color(0xFFE7F5EA) else AppSurface2
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (canPreview) Mint700 else Ink400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Xem trước",
                                color = if (canPreview) Mint700 else Ink400,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = {
            ComposerBottomBar(
                isEditing = isEditing,
                isSaving = uiState.isSaving,
                isPublishing = uiState.isPublishing,
                canPublish = titleFieldValue.text.isNotBlank(),
                onSaveDraft = {
                    viewModel.setTitle(titleFieldValue.text)
                    viewModel.saveDraft()
                },
                onPublish = {
                    viewModel.setTitle(titleFieldValue.text)
                    viewModel.publishPost()
                }
            )
        }
    ) { padding ->
        if (uiState.isLoadingBlog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Mint500)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 18.dp)
        ) {
            item {
                ComposerCoverUpload(
                    uiState = uiState,
                    onClick = { imageTarget = BlogImageTarget.Cover },
                    onClear = viewModel::clearCoverImage
                )
            }

            item {
                ComposerTitleField(
                    value = titleFieldValue,
                    onValueChange = { titleFieldValue = it },
                    onCommit = { viewModel.setTitle(titleFieldValue.text) }
                )
            }

            item {
                ComposerTagPicker(
                    selectedTags = uiState.tags,
                    availableTags = uiState.availableTags,
                    onAddTag = viewModel::addTag,
                    onRemoveTag = viewModel::removeTag
                )
            }

            item {
                ComposerContentHeader(blockCount = uiState.blocks.size)
            }

            // Content blocks
            itemsIndexed(uiState.blocks) { idx, block ->
                ContentBlockCard(
                    block = block,
                    index = idx,
                    total = uiState.blocks.size,
                    onTextChange = { viewModel.updateBlockText(block.id, it) },
                    onImageChange = { url, caption -> viewModel.updateBlockImage(block.id, url, caption) },
                    onPickImage = { imageTarget = BlogImageTarget.Block(block.id) },
                    onClearImage = { viewModel.clearBlockImage(block.id) },
                    onMoveUp = { viewModel.reorderBlock(block.id, -1) },
                    onMoveDown = { viewModel.reorderBlock(block.id, 1) },
                    onDelete = { viewModel.removeBlock(block.id) }
                )
            }

            // Add block button
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { viewModel.setShowAddBlockSheet(true) },
                    shape = RoundedCornerShape(VitalRadius.Pill),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5ECD9)),
                    shadowElevation = VitalElevation.Level1
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = Mint700, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm khối nội dung", color = Mint700, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        CancelComposerDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                showCancelDialog = false
                navController.popBackStack()
            }
        )
    }

    if (showPreview) {
        BlogComposerPreviewDialog(
            uiState = uiState.copy(title = titleFieldValue.text),
            onDismiss = { showPreview = false }
        )
    }

    // Add block bottom sheet
    if (uiState.showAddBlockSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowAddBlockSheet(false) },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = BottomSheetGrabber) },
            contentWindowInsets = { WindowInsets(0) }
        ) {
            Column(modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Text("Chọn loại khối", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BlockTypeOption(
                        icon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = Mint500, modifier = Modifier.size(28.dp)) },
                        label = "Text",
                        onClick = { viewModel.addBlock(ContentBlockType.TEXT) },
                        modifier = Modifier.weight(1f)
                    )
                    BlockTypeOption(
                        icon = { Icon(Icons.Default.Image, contentDescription = null, tint = MacroCarbs, modifier = Modifier.size(28.dp)) },
                        label = "Hình ảnh",
                        onClick = { viewModel.addBlock(ContentBlockType.IMAGE) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (imageTarget != null) {
        ImageSourceSheet(
            onDismiss = { imageTarget = null },
            onUrl = {
                urlTarget = imageTarget
                urlInput = when (val target = imageTarget) {
                    BlogImageTarget.Cover -> uiState.coverUrl
                    is BlogImageTarget.Block -> uiState.blocks.firstOrNull { it.id == target.blockId }?.imageUrl.orEmpty()
                    null -> ""
                }
                imageTarget = null
            },
            onDevice = {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    urlTarget?.let { target ->
        UrlImageDialog(
            value = urlInput,
            onValueChange = { urlInput = it },
            onDismiss = { urlTarget = null },
            onConfirm = {
                when (target) {
                    BlogImageTarget.Cover -> viewModel.setCoverUrl(urlInput)
                    is BlogImageTarget.Block -> {
                        val block = uiState.blocks.firstOrNull { it.id == target.blockId }
                        viewModel.updateBlockImage(target.blockId, urlInput, block?.caption.orEmpty())
                    }
                }
                urlTarget = null
            }
        )
    }
}

@Composable
private fun ComposerCoverUpload(uiState: BlogComposerUiState, onClick: () -> Unit, onClear: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (uiState.coverUrl.isBlank()) Color.Transparent else AppSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (uiState.coverUrl.isBlank()) Color(0xFFDDE9E2) else Color(0xFFE0EAE3)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(if (uiState.coverUrl.isBlank()) Color.Transparent else AppSurface),
            contentAlignment = Alignment.Center
        ) {
            val coverPreview = uiState.localCoverUri.ifBlank { uiState.coverUrl }
            if (coverPreview.isNotBlank()) {
                AsyncImage(
                    model = coverPreview,
                    contentDescription = "Ảnh bìa",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(VitalRadius.Pill),
                    color = Color.Black.copy(alpha = 0.42f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Thay ảnh bìa", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                ImageClearButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    onClick = onClear
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Color(0xFFE5F8EF)) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF19C287), modifier = Modifier.padding(16.dp).size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Thêm ảnh bìa", color = Ink700, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Khuyến nghị 16:9 · tối thiểu 1200×675", color = Ink400, fontSize = 13.sp)
                }
            }
        }
    }

}

private fun BlogComposerUiState.hasPreviewContent(): Boolean {
    return blocks.any { block ->
        when (block.type) {
            ContentBlockType.TEXT -> block.text.isNotBlank()
            ContentBlockType.IMAGE -> block.localImageUri.isNotBlank() || block.imageUrl.isNotBlank()
        }
    }
}

@Composable
private fun CancelComposerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hủy viết bài?", color = Ink900, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Text(
                "Nội dung chưa lưu có thể bị mất. Bạn có chắc muốn hủy không?",
                color = Ink700,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hủy bài", color = MacroProtein, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tiếp tục viết", color = Mint700, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = AppSurface
    )
}

@Composable
private fun BlogComposerPreviewDialog(
    uiState: BlogComposerUiState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppMutedBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .clickable(onClick = onDismiss),
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Color(0xFFF1F5F2)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng xem trước", tint = Ink900)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Xem trước", color = Ink900, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Bản nháp chưa được đăng", color = Ink500, fontSize = 12.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Color(0xFFE7F5EA)
                    ) {
                        Text(
                            "Preview",
                            color = Mint700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp)
                ) {
                    val coverPreview = uiState.localCoverUri.ifBlank { uiState.coverUrl }
                    if (coverPreview.isNotBlank()) {
                        item {
                            AsyncImage(
                                model = coverPreview,
                                contentDescription = "Ảnh bìa bài viết",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    item {
                        if (uiState.tags.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.tags.size) { index ->
                                    Surface(
                                        shape = RoundedCornerShape(VitalRadius.Pill),
                                        color = Color(0xFFD4F6E5)
                                    ) {
                                        Text(
                                            "#${uiState.tags[index]}",
                                            color = Mint700,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        Text(
                            uiState.title.ifBlank { "Tiêu đề bài viết của bạn..." },
                            color = if (uiState.title.isBlank()) Ink400 else Ink900,
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Bạn · Bản nháp", color = Ink500, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(18.dp))
                    }

                    val previewBlocks = uiState.blocks.filter { block ->
                        when (block.type) {
                            ContentBlockType.TEXT -> block.text.isNotBlank()
                            ContentBlockType.IMAGE -> block.localImageUri.isNotBlank() || block.imageUrl.isNotBlank()
                        }
                    }

                    if (previewBlocks.isEmpty()) {
                        item {
                            EmptyPreviewContent()
                        }
                    } else {
                        itemsIndexed(previewBlocks) { _, block ->
                            when (block.type) {
                                ContentBlockType.TEXT -> PreviewTextBlock(block.text)
                                ContentBlockType.IMAGE -> PreviewImageBlock(block)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPreviewContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0EAE3))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EditNote, contentDescription = null, tint = Mint700, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text("Chưa có nội dung để xem trước", color = Ink700, fontWeight = FontWeight.Bold)
            Text("Thêm đoạn văn bản hoặc hình ảnh để bài viết đầy đủ hơn.", color = Ink500, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PreviewTextBlock(text: String) {
    Text(
        text,
        color = Ink800,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        modifier = Modifier.padding(bottom = 18.dp)
    )
}

@Composable
private fun PreviewImageBlock(block: ContentBlock) {
    val imagePreview = block.localImageUri.ifBlank { block.imageUrl }
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        AsyncImage(
            model = imagePreview,
            contentDescription = block.caption.ifBlank { "Hình ảnh bài viết" },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
        if (block.caption.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(block.caption, color = Ink700, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceSheet(
    onDismiss: () -> Unit,
    onUrl: () -> Unit,
    onDevice: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppSurface
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text("Thêm ảnh", color = Ink900, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            ImageSourceOption(
                icon = Icons.Default.Link,
                title = "Thêm bằng URL",
                subtitle = "Dán link ảnh có sẵn từ internet",
                onClick = onUrl
            )
            Spacer(Modifier.height(10.dp))
            ImageSourceOption(
                icon = Icons.Default.PhotoLibrary,
                title = "Thêm ảnh từ thiết bị",
                subtitle = "Chọn ảnh trong thư viện của bạn",
                onClick = onDevice
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ImageSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF6FBF7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0EAE3))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Color(0xFFE5F8EF)) {
                Icon(icon, contentDescription = null, tint = Color(0xFF19C287), modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Ink900, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Ink500, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Ink400)
        }
    }
}

@Composable
private fun UrlImageDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dán URL ảnh", fontWeight = FontWeight.ExtraBold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Mint700) },
                placeholder = { Text("https://...") },
                shape = RoundedCornerShape(VitalRadius.Pill),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint500,
                    unfocusedBorderColor = Color(0xFFD5ECD9),
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotBlank()) {
                Text("Thêm ảnh", color = Mint700, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Ink500)
            }
        },
        containerColor = AppSurface
    )
}

@Composable
private fun ImageClearButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Pill),
        color = Color.Black.copy(alpha = 0.46f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

private fun Uri.toBase64DataUri(context: Context): String? {
    return try {
        val mimeType = context.contentResolver.getType(this) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(this)?.use { it.readBytes() } ?: return null
        "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun ComposerTitleField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onCommit: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "Tiêu đề bài viết của bạn...",
                fontSize = 29.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink400
            )
        },
        textStyle = LocalTextStyle.current.copy(
            fontSize = 29.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink900
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        minLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .onFocusChanged { if (!it.isFocused) onCommit() }
    )
}

@Composable
private fun ComposerTagPicker(
    selectedTags: List<String>,
    availableTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0EAE3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = Ink500, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(selectedTags.size) { i ->
                    ComposerTagChip(tag = selectedTags[i], onRemove = { onRemoveTag(selectedTags[i]) })
                }
                val selectableTags = availableTags.filterNot { available ->
                    selectedTags.any { selected -> selected.equals(available, ignoreCase = true) }
                }
                if (selectableTags.isEmpty()) {
                    item {
                        Text(
                            if (selectedTags.isEmpty()) "Chưa có tag" else "Hết tag",
                            color = Ink500,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(selectableTags.size) { i ->
                        ComposerAvailableTagChip(tag = selectableTags[i], onClick = { onAddTag(selectableTags[i]) })
                    }
                }
            }
        }
    }

}

@Composable
private fun ComposerContentHeader(blockCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("NỘI DUNG", color = Ink500, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        Text("$blockCount khối", color = Ink400, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ComposerBottomBar(
    isEditing: Boolean,
    isSaving: Boolean,
    isPublishing: Boolean,
    canPublish: Boolean,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit
) {
    Surface(
        color = AppSurface.copy(alpha = 0.96f),
        shadowElevation = VitalElevation.Level2
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .height(60.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .clickable(enabled = !isSaving, onClick = onSaveDraft),
                shape = RoundedCornerShape(VitalRadius.Pill),
                color = Color(0xFFF1F5F2)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = Mint500)
                    } else {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Ink700, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isEditing) "Lưu sửa" else "Lưu nháp",
                            color = Ink700,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .height(60.dp)
                    .weight(1.35f)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .clickable(enabled = !isPublishing && canPublish, onClick = onPublish),
                shape = RoundedCornerShape(VitalRadius.Pill),
                color = if (canPublish) Color(0xFF19C287) else Ink200
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (isEditing) "Cập nhật" else "Đăng bài",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun CoverComposerCard(uiState: BlogComposerUiState, onCoverUrlChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0EAE3)),
        shadowElevation = VitalElevation.Level1
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFBFE4C7), Color(0xFF257344)))),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = uiState.coverUrl,
                        contentDescription = "Ảnh bìa",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.04f),
                                    1f to Color.Black.copy(alpha = 0.58f)
                                )
                            )
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Color.White.copy(alpha = 0.78f)) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Mint700, modifier = Modifier.padding(16.dp).size(34.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Thêm ảnh bìa nổi bật", color = Ink900, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Ảnh này chỉ dùng để xem trước và đăng bài", color = Ink700, fontSize = 13.sp)
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    shape = RoundedCornerShape(VitalRadius.Pill),
                    color = Mint500
                ) {
                    Text(
                        "COVER",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.coverUrl,
                onValueChange = onCoverUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Mint700) },
                placeholder = { Text("URL ảnh bìa để xem trước", fontSize = 13.sp, color = Ink400) },
                shape = RoundedCornerShape(VitalRadius.Pill),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint500,
                    unfocusedBorderColor = Color(0xFFD5ECD9),
                    focusedContainerColor = Color(0xFFF6FBF7),
                    unfocusedContainerColor = Color(0xFFF6FBF7)
                )
            )
        }
    }
}

@Composable
private fun ComposerTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(Color(0xFFE7F5EA))
            .border(1.dp, Color(0xFFD5ECD9), RoundedCornerShape(VitalRadius.Pill))
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(tag, fontSize = 13.sp, color = Mint700, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Icon(
            Icons.Default.Close,
            contentDescription = "Xóa",
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(VitalRadius.Pill))
                .clickable(onClick = onRemove)
                .padding(2.dp),
            tint = Mint700
        )
    }
}

@Composable
private fun ComposerAvailableTagChip(tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(AppSurface)
            .border(1.dp, Color(0xFFD5ECD9), RoundedCornerShape(VitalRadius.Pill))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Mint700, modifier = Modifier.size(15.dp))
        Text(tag, fontSize = 13.sp, color = Mint700, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ComposerStatusStrip(uiState: BlogComposerUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        shadowElevation = VitalElevation.Level1
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Color(0xFFE7F5EA)) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = Mint700, modifier = Modifier.padding(9.dp).size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Blog cộng đồng", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Tạo bài viết theo style editorial của VitalAI", color = Ink500, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip("Bản nháp", Mint500)
                StatusChip("${uiState.blocks.size} khối", MacroWater)
                StatusChip("${uiState.tags.size} thẻ", MacroCarbs)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    uiState.error != null -> uiState.error.orEmpty()
                    uiState.savedDraft -> "Đã lưu nháp"
                    uiState.title.isBlank() -> "Chưa có tiêu đề"
                    else -> "Sẵn sàng đăng"
                },
                color = when {
                    uiState.error != null -> MacroProtein
                    uiState.title.isBlank() -> Ink500
                    else -> Mint700
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContentBlockCard(
    block: ContentBlock,
    index: Int,
    total: Int,
    onTextChange: (String) -> Unit,
    onImageChange: (String, String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var blockTextFieldValue by remember(block.id) { mutableStateOf(TextFieldValue(block.text)) }

    LaunchedEffect(blockTextFieldValue.text, blockTextFieldValue.composition) {
        if (blockTextFieldValue.composition == null && block.text != blockTextFieldValue.text) {
            onTextChange(blockTextFieldValue.text)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0EAE3)),
        elevation = CardDefaults.cardElevation(VitalElevation.Level1)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F7F4))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("...", color = Ink400, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${if (block.type == ContentBlockType.TEXT) "ĐOẠN VĂN BẢN" else "HÌNH ẢNH"} · #${index + 1}",
                    color = Ink700,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.weight(1f))
                BlockActionButton(icon = Icons.Default.KeyboardArrowUp, enabled = index > 0, contentDescription = "Lên", onClick = onMoveUp)
                Spacer(Modifier.width(8.dp))
                BlockActionButton(icon = Icons.Default.KeyboardArrowDown, enabled = index < total - 1, contentDescription = "Xuống", onClick = onMoveDown)
                Spacer(Modifier.width(8.dp))
                BlockActionButton(icon = Icons.Default.Close, tint = MacroProtein, contentDescription = "Xóa", onClick = onDelete)
            }

            when (block.type) {
                ContentBlockType.TEXT -> {
                    TextField(
                        value = blockTextFieldValue,
                        onValueChange = {
                            blockTextFieldValue = it
                        },
                        placeholder = { Text("Viết nội dung bài viết...", color = Ink300) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .onFocusChanged {
                                if (!it.isFocused) onTextChange(blockTextFieldValue.text)
                            },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppSurface,
                            unfocusedContainerColor = AppSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        minLines = 5
                    )
                }
                ContentBlockType.IMAGE -> {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val imagePreview = block.localImageUri.ifBlank { block.imageUrl }
                        if (imagePreview.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(266.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onPickImage)
                            ) {
                                AsyncImage(
                                    model = imagePreview,
                                    contentDescription = "Hình ảnh",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp),
                                    shape = RoundedCornerShape(VitalRadius.Pill),
                                    color = Color.Black.copy(alpha = 0.42f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Thay ảnh", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                ImageClearButton(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp),
                                    onClick = onClearImage
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(Mint50, Mint100)))
                                    .border(width = 1.dp, color = Color(0xFFD5ECD9), shape = RoundedCornerShape(18.dp))
                                    .clickable(onClick = onPickImage),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Mint700, modifier = Modifier.size(32.dp))
                                    Text("Thêm ảnh minh họa", color = Ink700, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = block.caption,
                            onValueChange = { onImageChange(block.imageUrl, it) },
                            placeholder = { Text("Chú thích ảnh...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(VitalRadius.Pill),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Mint500,
                                unfocusedBorderColor = Color(0xFFD5ECD9),
                                focusedContainerColor = Color(0xFFF6FBF7),
                                unfocusedContainerColor = Color(0xFFF6FBF7)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    tint: Color = Ink700,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0EAE3))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else Ink300,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BlockTypeOption(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface2),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 13.sp, color = Ink900, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BlogComposerScreenPreview() {
    val uiState = BlogComposerUiState(
        title = "Bài viết mới về sức khỏe",
        coverUrl = "",
        tags = listOf("Dinh dưỡng", "Tập luyện"),
        blocks = listOf(
            ContentBlock(id = "1", type = ContentBlockType.TEXT, text = "Nội dung bài viết mẫu..."),
            ContentBlock(id = "2", type = ContentBlockType.IMAGE, imageUrl = "", caption = "Ảnh minh họa")
        ),
        savedDraft = true
    )
    
    VitalAITheme {
        Scaffold(
            containerColor = AppMutedBackground,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Column {
                            Text("Bài viết mới", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink900)
                            Text("Soạn nội dung cộng đồng", fontSize = 12.sp, color = Ink500)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { ComposerStatusStrip(uiState = uiState) }
                item {
                    ContentBlockCard(
                        block = uiState.blocks[0],
                        index = 0, total = 2,
                        onTextChange = {}, onImageChange = { _, _ -> },
                        onPickImage = {},
                        onClearImage = {},
                        onMoveUp = {}, onMoveDown = {}, onDelete = {}
                    )
                }
                item {
                    ContentBlockCard(
                        block = uiState.blocks[1],
                        index = 1, total = 2,
                        onTextChange = {}, onImageChange = { _, _ -> },
                        onPickImage = {},
                        onClearImage = {},
                        onMoveUp = {}, onMoveDown = {}, onDelete = {}
                    )
                }
            }
        }
    }
}
