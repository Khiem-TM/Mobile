package com.vitalai.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = if (uiState.blog?.thumbnailUrl != null) Color.White else Ink900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadBlog(blogId) },
                modifier = Modifier.padding(padding)
            )
            uiState.blog != null -> BlogContent(blog = uiState.blog!!, paddingValues = padding)
        }
    }
}

@Composable
private fun BlogContent(blog: BlogDto, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero image
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            if (blog.thumbnailUrl != null) {
                AsyncImage(
                    model = blog.thumbnailUrl,
                    contentDescription = blog.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Mint500, Mint700)))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))
                    )
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Tags
            val tags = blog.tags
            if (!tags.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.take(3).forEach { tag ->
                        Surface(shape = RoundedCornerShape(999.dp), color = Mint50) {
                            Text(
                                tag,
                                color = Mint700,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Title
            Text(blog.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink900, lineHeight = 30.sp)

            Spacer(Modifier.height(12.dp))

            // Author + meta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Mint100),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        blog.displayAuthor.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                        fontWeight = FontWeight.Bold,
                        color = Mint700,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(blog.displayAuthor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink900)
                    Row {
                        Text(blog.createdAt.take(10), fontSize = 11.sp, color = Ink500)
                        if (blog.viewCount > 0) {
                            Text(" · ${blog.viewCount} lượt xem", fontSize = 11.sp, color = Ink500)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (blog.likesCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("❤️", fontSize = 13.sp)
                        Spacer(Modifier.width(3.dp))
                        Text("${blog.likesCount}", fontSize = 12.sp, color = Ink500)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Ink200)
            Spacer(Modifier.height(16.dp))

            // Render blocks if available, else fall back to plain content
            val blocks = blog.blocks
            if (!blocks.isNullOrEmpty()) {
                blocks.sortedBy { it.order }.forEach { block ->
                    BlogBlockView(block = block)
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                val text = blog.content ?: "Nội dung đang được cập nhật..."
                Text(text = text, fontSize = 15.sp, color = Ink700, lineHeight = 24.sp)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BlogBlockView(block: BlogBlockDto) {
    when (block.type) {
        "text" -> {
            if (!block.textContent.isNullOrBlank()) {
                Text(text = block.textContent, fontSize = 15.sp, color = Ink700, lineHeight = 24.sp)
            }
        }
        "image" -> {
            if (!block.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = block.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
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

    Scaffold(
        containerColor = AppBackground
    ) { padding ->

        BlogContent(
            blog = mockBlog,
            paddingValues = padding
        )
    }
}
