package com.vitalai.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.AuthorUserDto
import com.vitalai.data.remote.model.BlogBlockDto
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.CommentDto
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.SwipeToDeleteRow
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    blogId: String,
    navController: NavController,
    viewModel: BlogDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(blogId) {
        viewModel.loadBlog(blogId)
    }

    Scaffold(containerColor = AppMutedBackground) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadBlog(blogId) },
                modifier = Modifier.padding(padding)
            )
            uiState.blog != null -> BlogContent(
                blog = uiState.blog!!,
                comments = uiState.comments,
                isLiked = uiState.isLiked,
                isPostingComment = uiState.isPostingComment,
                paddingValues = padding,
                onBack = { navController.popBackStack() },
                onToggleLike = viewModel::toggleLike,
                onPostComment = viewModel::postComment,
                onDeleteComment = viewModel::deleteComment
            )
        }
    }
}

@Composable
private fun BlogContent(
    blog: BlogDto,
    comments: List<CommentDto>,
    isLiked: Boolean,
    isPostingComment: Boolean,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onToggleLike: () -> Unit,
    onPostComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(328.dp)) {
            if (blog.thumbnailUrl != null) {
                AsyncImage(
                    model = blog.thumbnailImage,
                    contentDescription = blog.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Mint300, Mint700))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(54.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.42f),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.58f)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VitalIconButton(
                    onClick = onBack,
                    containerColor = Color.Black.copy(alpha = 0.36f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroStatPill(icon = Icons.Default.Visibility, text = "${blog.viewCount}")
                    HeroStatPill(icon = Icons.Default.Favorite, text = "${blog.likesCount}")
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 50.dp)
            ) {
                blog.firstTag?.let {
                    Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Mint500.copy(alpha = 0.92f)) {
                        Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(blog.title, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 33.sp)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-28).dp),
            shape = RoundedCornerShape(VitalRadius.Xl),
            color = AppSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
            shadowElevation = VitalElevation.Level1
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val tags = blog.tags
                if (!tags.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.take(3).forEach { tag ->
                            Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Mint50) {
                                Text(
                                    tag,
                                    color = Mint700,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(Mint100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            blog.displayAuthor.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                            fontWeight = FontWeight.Bold,
                            color = Mint700,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(blog.displayAuthor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink900)
                        Text(blog.createdAt.take(10), fontSize = 12.sp, color = Ink500)
                    }
                    Spacer(Modifier.weight(1f))
                    DetailStat(icon = Icons.Default.Visibility, value = "${blog.viewCount}")
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onToggleLike,
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = if (isLiked) MacroProtein.copy(alpha = 0.14f) else AppSurface2
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Thích", tint = MacroProtein, modifier = Modifier.size(14.dp))
                            Text("${blog.likesCount}", color = Ink700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = AppLineSoft)
                Spacer(Modifier.height(18.dp))

                val blocks = blog.blocks
                if (!blocks.isNullOrEmpty()) {
                    blocks.sortedBy { it.order }.forEach { block ->
                        BlogBlockView(block = block)
                        Spacer(Modifier.height(14.dp))
                    }
                } else {
                    val text = blog.content ?: "Nội dung đang được cập nhật..."
                    Text(text = text, fontSize = 16.sp, color = Ink700, lineHeight = 25.sp)
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = AppLineSoft)
                Spacer(Modifier.height(16.dp))
                Text("Bình luận (${blog.commentCount})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Viết bình luận...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            onPostComment(commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank() && !isPostingComment
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Mint500)
                    }
                }
                Spacer(Modifier.height(10.dp))
                comments.forEach { comment ->
                    CommentItem(comment = comment, onDelete = { onDeleteComment(comment.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CommentItem(comment: CommentDto, onDelete: () -> Unit) {
    SwipeToDeleteRow(
        onDelete = onDelete,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VitalRadius.Md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurface)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Mint50),
                contentAlignment = Alignment.Center
            ) {
                Text(comment.authorUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U", color = Mint700, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(comment.authorUser?.displayName ?: "Người dùng", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink900)
                Text(comment.content, fontSize = 14.sp, color = Ink700, lineHeight = 20.sp)
                Text(comment.createdAt.take(10), fontSize = 11.sp, color = Ink500)
            }
        }
    }
}

@Composable
private fun BlogBlockView(block: BlogBlockDto) {
    when (block.type) {
        "text" -> {
            if (!block.textContent.isNullOrBlank()) {
                Text(text = block.textContent, fontSize = 16.sp, color = Ink700, lineHeight = 25.sp)
            }
        }
        "image" -> {
            if (!block.imageUrl.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    color = AppSurface2
                ) {
                    AsyncImage(
                        model = block.displayImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(Color.Black.copy(alpha = 0.36f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, color: Color = Mint500) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(AppSurface2)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(value, color = Ink700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BlogDetailScreenPreview() {
    val mockBlog = BlogDto(
        id = "1",
        title = "7 nguyên tắc eat clean giúp giảm mỡ hiệu quả",
        author = "VitalAI",
        content = null,
        thumbnailUrl = null,
        tags = listOf(
            "Dinh dưỡng",
            "Eat Clean",
            "Healthy"
        ),
        status = "published",
        likesCount = 328,
        viewCount = 1240,
        createdAt = "2026-05-13T10:00:00",
        authorUser = AuthorUserDto(
            id = "1",
            displayName = "VitalAI",
            email = "vitalai@example.com"
        ),
        blocks = listOf(
            BlogBlockDto(
                id = "1",
                order = 1,
                type = "text",
                textContent = """
                Eat clean là phương pháp ăn uống tập trung vào thực phẩm tự nhiên,
                hạn chế đồ chế biến sẵn và đường tinh luyện.
            """.trimIndent(),
                imageUrl = null
            ),
            BlogBlockDto(
                id = "2",
                order = 2,
                type = "image",
                textContent = null,
                imageUrl = null
            ),
            BlogBlockDto(
                id = "3",
                order = 3,
                type = "text",
                textContent = """
                Bạn nên ưu tiên:

                • Protein nạc
                • Rau xanh
                • Carb tốt
                • Uống đủ nước

                Việc duy trì chế độ ăn đều đặn sẽ giúp cải thiện sức khỏe và giảm mỡ bền vững.
            """.trimIndent(),
                imageUrl = null
            )
        )
    )

    VitalAITheme {
        Scaffold(
            containerColor = AppBackground
        ) { padding ->
            BlogContent(
                blog = mockBlog,
                comments = emptyList(),
                isLiked = false,
                isPostingComment = false,
                paddingValues = padding,
                onBack = {},
                onToggleLike = {},
                onPostComment = {},
                onDeleteComment = {}
            )
        }
    }
}
