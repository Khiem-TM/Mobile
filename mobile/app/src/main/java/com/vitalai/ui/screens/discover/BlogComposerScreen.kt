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
import com.vitalai.ui.components.SectionHeader
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
                title = {
                    Column {
                        Text("Bài viết mới", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink900)
                        Text("Soạn nội dung cộng đồng", fontSize = 12.sp, color = Ink500)
                    }
                },
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
            Surface(shadowElevation = VitalElevation.Level2, color = AppSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
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
            item {
                ComposerStatusStrip(uiState = uiState)
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(VitalRadius.Xl),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
                    shadowElevation = VitalElevation.Level1
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(VitalRadius.Lg))
                                .background(AppSurface2),
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
                                    Text("Dán URL ảnh bìa bên dưới", color = Ink300, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.coverUrl,
                            onValueChange = viewModel::setCoverUrl,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("URL ảnh bìa", fontSize = 13.sp, color = Ink400) },
                            shape = RoundedCornerShape(VitalRadius.Md),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Mint500,
                                unfocusedBorderColor = AppLine,
                                focusedContainerColor = AppSurface,
                                unfocusedContainerColor = AppSurface
                            )
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
                ) {
                    TextField(
                        value = uiState.title,
                        onValueChange = viewModel::setTitle,
                        placeholder = {
                            Text("Tiêu đề bài viết...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink300)
                        },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
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
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
                ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
private fun ComposerStatusStrip(uiState: BlogComposerUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(VitalRadius.Pill),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip("Bản nháp", Mint500)
            StatusChip("${uiState.blocks.size} khối", MacroWater)
            StatusChip("${uiState.tags.size} thẻ", MacroCarbs)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    uiState.error != null -> uiState.error.orEmpty()
                    uiState.savedDraft -> "Đã lưu nháp"
                    uiState.title.isBlank() -> "Chưa có tiêu đề"
                    else -> "Tự động đăng"
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
                            value = block.imageUrl,
                            onValueChange = { onImageChange(it, block.caption) },
                            placeholder = { Text("URL hình ảnh...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(VitalRadius.Md),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Mint500,
                                unfocusedBorderColor = AppLine
                            )
                        )
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
