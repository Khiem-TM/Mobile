package com.vitalai.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogComposerScreen(
    navController: NavController,
    viewModel: BlogComposerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    // Navigate away after publish
    LaunchedEffect(uiState.published) {
        if (uiState.published) navController.popBackStack()
    }

    Scaffold(
        containerColor = AppMutedBackground,
        topBar = {
            TopAppBar(
                title = { Text("Bài viết mới", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                actions = {
                    TextButton(onClick = {}) {
                        Text("Xem trước", color = Mint500, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = AppSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::saveDraft,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Mint500)
                        } else {
                            Text("Lưu nháp", color = Ink700)
                        }
                    }
                    Button(
                        onClick = viewModel::publishPost,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isPublishing && uiState.title.isNotBlank(),
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint500)
                    ) {
                        if (uiState.isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Đăng bài", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Cover image upload area
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(VitalRadius.Xl))
                        .run {
                            if (uiState.coverUrl.isBlank()) {
                                border(width = 1.5.dp, color = AppLine, shape = RoundedCornerShape(VitalRadius.Xl))
                                    .background(AppSurface)
                            } else {
                                this
                            }
                        }
                        .clickable { /* image picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = uiState.coverUrl,
                            contentDescription = "Ảnh bìa",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Ink300, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Thêm ảnh bìa", color = Ink500, fontSize = 14.sp)
                            Text("Tỉ lệ 16:9 tốt nhất", color = Ink300, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Title field
            item {
                TextField(
                    value = uiState.title,
                    onValueChange = viewModel::setTitle,
                    placeholder = {
                        Text("Tiêu đề bài viết...", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink200)
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            // Tags section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Thẻ", fontSize = 13.sp, color = Ink700, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.tags.size) { i ->
                            val tag = uiState.tags[i]
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VitalRadius.Pill))
                                    .background(Mint50)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tag, fontSize = 12.sp, color = Mint700)
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeTag(tag) },
                                    tint = Mint700
                                )
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = uiState.tagInput,
                                    onValueChange = viewModel::setTagInput,
                                    placeholder = { Text("Thêm thẻ...", fontSize = 12.sp) },
                                    modifier = Modifier.width(120.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(VitalRadius.Pill),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Mint500,
                                        unfocusedBorderColor = Ink200
                                    )
                                )
                                IconButton(
                                    onClick = { viewModel.addTag(uiState.tagInput) },
                                    enabled = uiState.tagInput.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Mint500)
                                }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = Ink200, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Text(
                    "Nội dung",
                    fontSize = 13.sp,
                    color = Ink700,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
            containerColor = AppSurface
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Block header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurface2)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DragHandle, contentDescription = null, tint = Ink300, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (block.type == ContentBlockType.TEXT) "Văn bản" else "Hình ảnh",
                    fontSize = 11.sp,
                    color = Ink500,
                    modifier = Modifier.weight(1f)
                )
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

            // Block body
            when (block.type) {
                ContentBlockType.TEXT -> {
                    TextField(
                        value = block.text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Nhập nội dung...", color = Ink300) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        minLines = 3
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
                                    .clip(RoundedCornerShape(VitalRadius.Md))
                                    .background(AppSurface2)
                                    .border(width = 1.dp, color = AppLine, shape = RoundedCornerShape(VitalRadius.Md))
                                    .clickable { /* pick image */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Ink300, modifier = Modifier.size(32.dp))
                                    Text("Chọn ảnh", color = Ink500, fontSize = 13.sp)
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
                            shape = RoundedCornerShape(VitalRadius.Md),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Mint500,
                                unfocusedBorderColor = Ink200
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
