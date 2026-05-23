package com.vitalai.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vitalai.data.remote.model.AuthorUserDto
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.VitalBottomNavBar
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tags = listOf(null to "Tất cả", "Dinh dưỡng" to "Dinh dưỡng", "Tập luyện" to "Tập luyện", "Mindset" to "Mindset", "Công thức" to "Công thức")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khám phá", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.BlogComposer) }) {
                        Text("Viết bài", color = Mint500, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tag filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurface)
                    .padding(vertical = 8.dp)
            ) {
                items(tags) { (key, label) ->
                    val isSelected = uiState.selectedTag == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterByTag(key) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Mint900,
                            selectedLabelColor = Color.White,
                            containerColor = AppSurface,
                            labelColor = Ink700
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Ink200,
                            selectedBorderColor = Mint900
                        )
                    )
                }
            }

            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(message = uiState.error!!, onRetry = viewModel::loadBlogs)
                uiState.blogs.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📰", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Chưa có bài viết nào", color = Ink500)
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    // Featured first item
                    item {
                        val featured = uiState.blogs.first()
                        FeaturedBlogCard(
                            blog = featured,
                            onClick = { navController.navigate(Screen.BlogDetail(featured.id)) }
                        )
                    }
                    // Remaining items
                    items(uiState.blogs.drop(1)) { blog ->
                        BlogListItem(
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
private fun FeaturedBlogCard(blog: BlogDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                blog.firstTag?.let {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Mint500.copy(alpha = 0.9f)
                    ) {
                        Text(it, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    blog.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BlogListItem(blog: BlogDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (blog.thumbnailUrl != null) {
            AsyncImage(
                model = blog.thumbnailUrl,
                contentDescription = blog.title,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Mint50),
                contentAlignment = Alignment.Center
            ) { Text("📰", fontSize = 28.sp) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            blog.firstTag?.let {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Mint50
                ) {
                    Text(it, color = Mint700, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Spacer(Modifier.height(3.dp))
            }
            Text(
                blog.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Ink900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(blog.displayAuthor, fontSize = 11.sp, color = Ink500)
        }
    }
    HorizontalDivider(color = Ink200, thickness = 0.5.dp)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiscoverScreenPreview() {

    val featuredBlog = BlogDto(
        id = "1",
        title = "7 nguyên tắc eat clean giúp giảm mỡ hiệu quả",
        author = "VitalAI",
        content = null,
        thumbnailUrl = null,
        tags = listOf(
            "Dinh dưỡng",
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

        blocks = null
    )

    val blogs = listOf(
        featuredBlog,

        BlogDto(
            id = "2",
            title = "Cardio hay gym tốt hơn cho giảm cân?",
            author = "Coach Vita",
            content = null,
            thumbnailUrl = null,
            tags = listOf("Tập luyện"),
            status = "published",
            likesCount = 120,
            viewCount = 830,
            createdAt = "2026-05-13T12:00:00",

            authorUser = AuthorUserDto(
                id = "2",
                displayName = "Coach Vita",
                email = "coach@example.com"
            ),

            blocks = null
        ),

        BlogDto(
            id = "3",
            title = "Meal prep cho người bận rộn",
            author = "Healthy Team",
            content = null,
            thumbnailUrl = null,
            tags = listOf("Công thức"),
            status = "published",
            likesCount = 89,
            viewCount = 450,
            createdAt = "2026-05-13T14:00:00",

            authorUser = AuthorUserDto(
                id = "3",
                displayName = "Healthy Team",
                email = "healthy@example.com"
            ),

            blocks = null
        )
    )

    Scaffold(
        containerColor = AppBackground
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {

                items(
                    listOf(
                        "Tất cả",
                        "Dinh dưỡng",
                        "Tập luyện",
                        "Mindset"
                    )
                ) { label ->

                    FilterChip(
                        selected = label == "Tất cả",
                        onClick = {},
                        label = {
                            Text(label)
                        }
                    )
                }
            }

            LazyColumn {

                item {
                    FeaturedBlogCard(
                        blog = featuredBlog,
                        onClick = {}
                    )
                }

                items(blogs.drop(1)) { blog ->

                    BlogListItem(
                        blog = blog,
                        onClick = {}
                    )
                }
            }
        }
    }
}
