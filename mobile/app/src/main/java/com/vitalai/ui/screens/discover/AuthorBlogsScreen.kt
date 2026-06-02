package com.vitalai.ui.screens.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.blog.BlogAuthorAvatar
import com.vitalai.ui.components.blog.BlogCover
import com.vitalai.ui.components.blog.BlogStatPill
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorBlogsScreen(
    authorId: String,
    authorName: String,
    avatarUrl: String?,
    navController: NavController,
    viewModel: AuthorBlogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayName = authorName.ifBlank { "Tác giả" }

    LaunchedEffect(authorId) {
        viewModel.loadBlogs(authorId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, fontWeight = FontWeight.Bold, color = Ink900) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppMutedBackground
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadBlogs(authorId) },
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AuthorHeader(
                        name = displayName,
                        avatarUrl = avatarUrl,
                        postCount = uiState.blogs.size
                    )
                }
                item {
                    Text("Bài viết mới nhất", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink900)
                }
                if (uiState.blogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tác giả này chưa có bài viết nào", color = Ink500)
                        }
                    }
                } else {
                    items(uiState.blogs, key = { it.id }) { blog ->
                        AuthorBlogItem(
                            blog = blog,
                            onClick = { navController.navigate(Screen.BlogDetail(blog.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorHeader(name: String, avatarUrl: String?, postCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VitalRadius.Xl),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlogAuthorAvatar(name = name, avatarUrl = avatarUrl, size = 58.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink900)
                Text("$postCount bài viết", color = Ink500, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AuthorBlogItem(blog: BlogDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Xl),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BlogCover(
                blog = blog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                radius = VitalRadius.Lg
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                blog.firstTag?.let { tag ->
                    Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Mint50) {
                        Text(
                            tag,
                            color = Mint700,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(blog.createdAt.take(10), color = Ink500, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                blog.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Ink900,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val preview = blog.content
                ?: blog.blocks
                    ?.sortedBy { it.order }
                    ?.firstOrNull { !it.textContent.isNullOrBlank() }
                    ?.textContent
            if (!preview.isNullOrBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    preview,
                    color = Ink700,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    BlogStatPill(Icons.Default.Visibility, "${blog.viewCount}")
                    BlogStatPill(Icons.Default.Favorite, "${blog.likesCount}", color = MacroProtein)
                    BlogStatPill(Icons.Default.ChatBubbleOutline, "${blog.commentCount}")
                }
                Text("Đọc tiếp", color = Mint700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
