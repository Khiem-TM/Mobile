package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.screens.discover.viewmodels.BlogComposerUiState
import com.vitalai.ui.screens.discover.viewmodels.BlogComposerViewModel
import com.vitalai.ui.screens.discover.viewmodels.ContentBlock
import com.vitalai.ui.screens.discover.viewmodels.ContentBlockType
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogComposerScreen(
    navController: NavController,
    blogId: String? = null,
    viewModel: BlogComposerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val isEditing = uiState.editingBlogId != null

    LaunchedEffect(blogId) {
        viewModel.loadBlogForEdit(blogId)
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .clickable(onClick = { }),
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Color(0xFFE7F5EA)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Mint700, modifier = Modifier.size(16.dp))
                            Text(
                                "Xem trước",
                                color = Mint700,
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
                canPublish = uiState.title.isNotBlank(),
                onSaveDraft = viewModel::saveDraft,
                onPublish = viewModel::publishPost
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
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                ComposerStatusStrip(uiState = uiState)
            }

            item {
                CoverComposerCard(uiState = uiState, onCoverUrlChange = viewModel::setCoverUrl)
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
                    shadowElevation = VitalElevation.Level1
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Thông tin bài viết", fontSize = 13.sp, color = Mint700, fontWeight = FontWeight.Bold)
                        TextField(
                            value = uiState.title,
                            onValueChange = viewModel::setTitle,
                            placeholder = {
                                Text("Tiêu đề bài viết...", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink300)
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 24.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Ink900
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Chủ đề", fontSize = 13.sp, color = Ink700, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.tags.size) { i ->
                                val tag = uiState.tags[i]
                                ComposerTagChip(tag = tag, onRemove = { viewModel.removeTag(tag) })
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = uiState.tagInput,
                                        onValueChange = viewModel::setTagInput,
                                        placeholder = { Text("Thêm thẻ", fontSize = 12.sp) },
                                        modifier = Modifier.width(132.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(VitalRadius.Pill),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Mint500,
                                            unfocusedBorderColor = Color(0xFFD5ECD9),
                                            focusedContainerColor = Color(0xFFF3FAF5),
                                            unfocusedContainerColor = Color(0xFFF3FAF5)
                                        )
                                    )
                                    IconButton(
                                        onClick = { viewModel.addTag(uiState.tagInput) },
                                        enabled = uiState.tagInput.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "Thêm", tint = if (uiState.tagInput.isNotBlank()) Mint500 else Ink300)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Nội dung",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Content blocks
            itemsIndexed(uiState.blocks) { idx, block ->
                ContentBlockCard(
                    block = block,
                    index = idx,
                    total = uiState.blocks.size,
                    onTextChange = { viewModel.updateBlockText(block.id, it) },
                    onImageChange = { url, caption -> viewModel.updateBlockImage(block.id, url, caption) },
                    onMoveUp = { viewModel.reorderBlock(block.id, -1) },
                    onMoveDown = { viewModel.reorderBlock(block.id, 1) },
                    onDelete = { viewModel.removeBlock(block.id) }
                )
            }

            // Add block button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(VitalRadius.Md))
                        .border(width = 1.5.dp, color = AppLine, shape = RoundedCornerShape(VitalRadius.Md))
                        .clickable { viewModel.setShowAddBlockSheet(true) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Ink500)
                        Text("Thêm khối nội dung", color = Ink500, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Add block bottom sheet
    if (uiState.showAddBlockSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowAddBlockSheet(false) },
            sheetState = sheetState,
            containerColor = AppSurface,
            windowInsets = WindowInsets(0)
        ) {
            Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                    .height(46.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .clickable(enabled = !isSaving, onClick = onSaveDraft),
                shape = RoundedCornerShape(VitalRadius.Pill),
                color = AppSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
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
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (isEditing) "Lưu sửa" else "Lưu nháp",
                            color = Ink700,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .height(46.dp)
                    .weight(1.05f)
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
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isEditing) "Cập nhật" else "Đăng bài",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        shadowElevation = VitalElevation.Level1
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Mint100, Mint500))),
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
                        Text("Dán URL ảnh để hiển thị như hero blog", color = Ink700, fontSize = 13.sp)
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

                if (uiState.title.isNotBlank()) {
                    Text(
                        uiState.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                placeholder = { Text("URL ảnh bìa", fontSize = 13.sp, color = Ink400) },
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
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(VitalElevation.Level1)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6FBF7))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = if (block.type == ContentBlockType.TEXT) Mint100 else Color(0xFFFFF1D6)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (block.type == ContentBlockType.TEXT) Icons.Default.TextFields else Icons.Default.Image,
                            contentDescription = null,
                            tint = if (block.type == ContentBlockType.TEXT) Mint700 else MacroCarbs,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            if (block.type == ContentBlockType.TEXT) "Văn bản" else "Hình ảnh",
                            fontSize = 12.sp,
                            color = if (block.type == ContentBlockType.TEXT) Mint700 else MacroCarbs,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (index > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Lên", tint = Ink500, modifier = Modifier.size(16.dp))
                    }
                }
                if (index < total - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Xuống", tint = Ink500, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MacroProtein, modifier = Modifier.size(16.dp))
                }
            }

            when (block.type) {
                ContentBlockType.TEXT -> {
                    TextField(
                        value = block.text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Nhập nội dung...", color = Ink300) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppSurface,
                            unfocusedContainerColor = AppSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        minLines = 4
                    )
                }
                ContentBlockType.IMAGE -> {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (block.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = block.imageUrl,
                                contentDescription = "Hình ảnh",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(VitalRadius.Md)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(Mint50, Mint100)))
                                    .border(width = 1.dp, color = Color(0xFFD5ECD9), shape = RoundedCornerShape(18.dp))
                                    .clickable { /* pick image */ },
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
                            value = block.imageUrl,
                            onValueChange = { onImageChange(it, block.caption) },
                            placeholder = { Text("URL hình ảnh...", fontSize = 12.sp) },
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
                        onMoveUp = {}, onMoveDown = {}, onDelete = {}
                    )
                }
                item {
                    ContentBlockCard(
                        block = uiState.blocks[1],
                        index = 1, total = 2,
                        onTextChange = {}, onImageChange = { _, _ -> },
                        onMoveUp = {}, onMoveDown = {}, onDelete = {}
                    )
                }
            }
        }
    }
}
