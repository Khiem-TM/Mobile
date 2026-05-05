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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    blogId: String,
    navController: NavController,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val blog = uiState.blogs.find { it.id == blogId }
    var isLoading by remember { mutableStateOf(blog == null) }

    LaunchedEffect(blogId) {
        if (blog == null) {
            // Try to load blog from repository via viewModel
            // For now just mark loading done
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        when {
            isLoading -> LoadingState(modifier = Modifier.padding(padding))
            blog == null -> ErrorState(
                message = "Không tìm thấy bài viết",
                onRetry = { navController.popBackStack() },
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (blog.imageUrl != null) {
                        AsyncImage(
                            model = blog.imageUrl,
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
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Tag
                    blog.tag?.let {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Mint50
                        ) {
                            Text(
                                it,
                                color = Mint700,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Title
                    Text(
                        blog.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Author row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Mint100),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (blog.authorName?.firstOrNull() ?: 'A').uppercaseChar().toString(),
                                fontWeight = FontWeight.Bold,
                                color = Mint700
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                blog.authorName ?: "VitalAI",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Ink900
                            )
                            Row {
                                Text(blog.createdAt.take(10), fontSize = 11.sp, color = Ink500)
                                blog.readTimeMin?.let {
                                    Text(" · $it phút đọc", fontSize = 11.sp, color = Ink500)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink200)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Content
                    Text(
                        text = blog.content ?: blog.summary ?: "Nội dung đang được cập nhật...",
                        fontSize = 15.sp,
                        color = Ink700,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
