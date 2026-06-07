package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.screens.discover.components.BlogAuthorAvatar
import com.vitalai.ui.screens.discover.components.BlogCover
import com.vitalai.ui.screens.discover.viewmodels.BlogDetailViewModel
import com.vitalai.ui.theme.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    blogId: String,
    navController: NavController,
    viewModel: BlogDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(blogId) {
        viewModel.loadBlog(blogId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Chi tiết bài viết", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink800)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = {
            if (uiState.blog != null) {
                BlogCommentInputBar(
                    value = commentText,
                    onValueChange = { commentText = it },
                    isPosting = uiState.isPostingComment,
                    onSend = {
                        viewModel.postComment(commentText)
                        commentText = ""
                    }
                )
            }
        },
        containerColor = AppMutedBackground
    ) { padding ->
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
                paddingValues = padding,
                onOpenAuthor = { blog ->
                    blog.authorUser?.let { author ->
                        navController.navigate(
                            Screen.AuthorBlogs(
                                authorId = author.id,
                                authorName = blog.displayAuthor,
                                avatarUrl = author.displayAvatarUrl
                            )
                        )
                    }
                },
                onToggleLike = viewModel::toggleLike,
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
    paddingValues: PaddingValues,
    onOpenAuthor: (BlogDto) -> Unit,
    onToggleLike: () -> Unit,
    onDeleteComment: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                .height(196.dp)
                .clip(RoundedCornerShape(VitalRadius.Xl))
        ) {
            BlogCover(
                blog = blog,
                modifier = Modifier.fillMaxSize(),
                radius = VitalRadius.Xl,
                fallbackGradient = true
            )
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = "Lưu bài",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(28.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(VitalRadius.Xl),
            color = AppSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
            shadowElevation = VitalElevation.Level1
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val tags = blog.tags
                if (!tags.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tags.take(2).forEach { tag ->
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
                        Spacer(Modifier.weight(1f))
                        Text("${blog.createdAt.take(10)} · ${blog.viewCount} views", color = Ink500, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(blog.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink900, lineHeight = 30.sp)
                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlogAuthorAvatar(
                        name = blog.displayAuthor,
                        avatarUrl = blog.authorUser?.displayAvatarUrl,
                        modifier = Modifier
                            .clickable(enabled = blog.authorUser != null) { onOpenAuthor(blog) },
                        size = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = blog.authorUser != null) { onOpenAuthor(blog) }
                    ) {
                        Text(blog.displayAuthor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink900)
                        Text(blog.createdAt.take(10), fontSize = 12.sp, color = Ink500)
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
            }
        }
        BlogEngagementBar(
            blog = blog,
            commentsCount = maxOf(blog.commentCount, comments.size),
            isLiked = isLiked,
            onToggleLike = onToggleLike
        )
        BlogCommentsSection(
            commentsCount = maxOf(blog.commentCount, comments.size),
            comments = comments,
            onDeleteComment = onDeleteComment
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun BlogEngagementBar(
    blog: BlogDto,
    commentsCount: Int,
    isLiked: Boolean,
    onToggleLike: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        color = AppSurface,
        shadowElevation = VitalElevation.Level1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EngagementAction(
                icon = Icons.Default.Favorite,
                label = "${blog.likesCount}",
                selected = isLiked,
                onClick = onToggleLike
            )
            Spacer(Modifier.width(24.dp))
            EngagementAction(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "$commentsCount",
                selected = false,
                onClick = {}
            )
            Spacer(Modifier.width(24.dp))
            EngagementAction(
                icon = Icons.Default.Share,
                label = "Share",
                selected = false,
                onClick = {}
            )
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = AppSurface2
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Lưu bài", tint = Ink800, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EngagementAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) MacroProtein else Ink800, modifier = Modifier.size(22.dp))
        Text(label, color = Ink800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BlogCommentsSection(
    commentsCount: Int,
    comments: List<CommentDto>,
    onDeleteComment: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Text("Bình luận · $commentsCount", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink900)
            Spacer(Modifier.height(16.dp))
            if (comments.isEmpty()) {
                Text("Chưa có bình luận nào.", color = Ink500, fontSize = 14.sp)
            } else {
                comments.forEach { comment ->
                    CommentItem(comment = comment, onDelete = { onDeleteComment(comment.id) })
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun BlogCommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isPosting: Boolean,
    onSend: () -> Unit
) {
    Surface(
        color = AppSurface,
        shadowElevation = VitalElevation.Level2
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppSurface2),
                contentAlignment = Alignment.Center
            ) {
                Text("T", color = Ink700, fontWeight = FontWeight.Bold)
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Viết bình luận...", color = Ink400, fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(VitalRadius.Pill),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppSurface2,
                    unfocusedContainerColor = AppSurface2,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(enabled = value.isNotBlank() && !isPosting, onClick = onSend),
                shape = CircleShape,
                color = if (value.isNotBlank()) Color(0xFF19C287) else AppSurface2
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi bình luận", tint = if (value.isNotBlank()) Color.White else Ink400)
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: CommentDto, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(Mint50),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.authorUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U", color = Mint700, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorUser?.displayName ?: "Người dùng", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink900)
                Spacer(Modifier.width(8.dp))
                Text(comment.createdAt.relativeTimeLabel(), fontSize = 13.sp, color = Ink500)
            }
            Spacer(Modifier.height(6.dp))
            Text(comment.content, fontSize = 15.sp, color = Ink700, lineHeight = 22.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Ink500, modifier = Modifier.size(14.dp))
                    Text("0", fontSize = 12.sp, color = Ink500, fontWeight = FontWeight.SemiBold)
                }
                Text("Trả lời", fontSize = 12.sp, color = Ink500, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Xóa bình luận", tint = Ink300, modifier = Modifier.size(15.dp))
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
            val imageUrl = block.displayImageUrl
            if (imageUrl != null) {
                Surface(
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    color = AppSurface2
                ) {
                    AsyncImage(
                        model = imageUrl,
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

private fun String.relativeTimeLabel(): String {
    val instant = runCatching { Instant.parse(this) }.getOrElse {
        runCatching {
            LocalDateTime.parse(take(19)).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
    } ?: return take(10)

    val duration = Duration.between(instant, Instant.now())
    return when {
        duration.toMinutes() < 1 -> "vừa xong"
        duration.toHours() < 1 -> "${duration.toMinutes()}m"
        duration.toDays() < 1 -> "${duration.toHours()}h"
        duration.toDays() < 7 -> "${duration.toDays()}d"
        else -> take(10)
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
                paddingValues = padding,
                onOpenAuthor = {},
                onToggleLike = {},
                onDeleteComment = {}
            )
        }
    }
}
