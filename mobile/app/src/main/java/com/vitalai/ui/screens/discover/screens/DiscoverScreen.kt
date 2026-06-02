package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.components.VitalPill
import com.vitalai.ui.screens.discover.components.BlogCover
import com.vitalai.ui.screens.discover.components.BlogMetaRow
import com.vitalai.ui.screens.discover.components.BlogSearchBar
import com.vitalai.ui.screens.discover.components.BlogStatPill
import com.vitalai.ui.screens.discover.viewmodels.DiscoverViewModel
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    viewModel: DiscoverViewModel = hiltViewModel(),
    showBackButton: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val tags = remember(uiState.tags, uiState.blogs) {
        val fallbackTags = listOf("Dinh dưỡng", "Tập luyện", "Sức khỏe", "Giảm cân", "Eat clean")
        val blogTags = uiState.blogs.flatMap { it.tags.orEmpty() }
        val mergedTags = (uiState.tags + blogTags + fallbackTags).distinctBy { it.lowercase() }
        listOf(null to "Tất cả") + mergedTags.map { it to it }
    }
    val searchedBlogs = remember(uiState.blogs, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            uiState.blogs
        } else {
            uiState.blogs.filter { blog ->
                blog.title.contains(query, ignoreCase = true) ||
                    blog.displayAuthor.contains(query, ignoreCase = true) ||
                    blog.tags.orEmpty().any { it.contains(query, ignoreCase = true) }
            }
        }
    }
    val latestBlogs = remember(searchedBlogs, uiState.selectedTag) {
        val tag = uiState.selectedTag
        if (tag == null) {
            searchedBlogs
        } else {
            searchedBlogs.filter { blog ->
                blog.tags.orEmpty().any { it.equals(tag, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BlogSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Thông báo", tint = Ink700)
                    }
                    VitalIconButton(
                        onClick = { navController.navigate(Screen.BlogComposer()) },
                        modifier = Modifier.padding(end = 12.dp),
                        containerColor = Mint500
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Viết bài", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppMutedBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(message = uiState.error!!, onRetry = viewModel::loadBlogs)
                searchedBlogs.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📰", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Chưa có bài viết nào", color = Ink500)
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                    item {
                        Text(
                            "Recommended",
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink900
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchedBlogs.take(5), key = { it.id }) { blog ->
                                FeaturedBlogCard(
                                    blog = blog,
                                    onClick = { navController.navigate(Screen.BlogDetail(blog.id)) }
                                )
                            }
                        }
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)
                        ) {
                            items(tags) { (key, label) ->
                                val isSelected = uiState.selectedTag == key
                                VitalPill(
                                    text = label,
                                    selected = isSelected,
                                    mint = !isSelected,
                                    onClick = { viewModel.filterByTag(key) },
                                )
                            }
                        }
                    }

                    if (uiState.exploreFoods.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Món nên thử",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.exploreFoods, key = { it.id }) { food ->
                                    ExploreFoodCard(
                                        food = food,
                                        onClick = { navController.navigate(Screen.FoodDetail(food.id)) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Bài viết mới",
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    if (latestBlogs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có bài viết cho tag này", color = Ink500, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(latestBlogs, key = { it.id }) { blog ->
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
}

@Composable
private fun ExploreFoodCard(food: FoodDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(156.dp)
            .height(184.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
    ) {
        Column {
            if (food.imageUrl != null) {
                AsyncImage(
                    model = food.imageUrl,
                    contentDescription = food.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Mint50),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽️", fontSize = 28.sp)
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(food.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Ink900)
                Text("${food.caloriesPer100g.toInt()} kcal/100g", fontSize = 11.sp, color = Ink500)
            }
        }
    }
}

@Composable
private fun FeaturedBlogCard(blog: BlogDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(286.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(VitalRadius.Xl))
            ) {
                BlogCover(
                    blog = blog,
                    modifier = Modifier.fillMaxSize(),
                    radius = VitalRadius.Xl,
                    fallbackGradient = true
                )
                blog.firstTag?.let {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Color.White.copy(alpha = 0.82f)
                    ) {
                        Text(it, color = Ink700, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = "Lưu bài",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(25.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                blog.title,
                modifier = Modifier.padding(horizontal = 2.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Ink900,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            BlogMetaRow(blog = blog, modifier = Modifier.padding(horizontal = 2.dp))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlogStatPill(icon = Icons.Default.Visibility, value = "${blog.viewCount}")
                BlogStatPill(icon = Icons.Default.Favorite, value = "${blog.likesCount}", color = MacroProtein)
                BlogStatPill(icon = Icons.Default.ChatBubbleOutline, value = "${blog.commentCount}", color = Mint600)
            }
        }
    }
}

@Composable
private fun BlogListItem(blog: BlogDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlogCover(
                blog = blog,
                modifier = Modifier.size(width = 96.dp, height = 108.dp),
                radius = VitalRadius.Md
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                blog.firstTag?.let {
                    Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = Mint50) {
                        Text(it, color = Mint700, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    blog.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Ink900,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                BlogMetaRow(blog = blog)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlogStatPill(icon = Icons.Default.Visibility, value = "${blog.viewCount}")
                    BlogStatPill(icon = Icons.Default.Favorite, value = "${blog.likesCount}", color = MacroProtein)
                    BlogStatPill(icon = Icons.Default.ChatBubbleOutline, value = "${blog.commentCount}", color = Mint600)
                }
            }
        }
    }
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

    VitalAITheme {
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
}
