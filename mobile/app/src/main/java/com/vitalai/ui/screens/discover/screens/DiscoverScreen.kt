package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Notifications
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
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.screens.discover.components.BlogCover
import com.vitalai.ui.screens.discover.components.BlogSearchBar
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
                        FeaturedBlogCard(
                            blog = searchedBlogs.first(),
                            onClick = { navController.navigate(Screen.BlogDetail(searchedBlogs.first().id)) }
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)
                        ) {
                            items(tags) { (key, label) ->
                                val isSelected = uiState.selectedTag == key
                                BlogTagChip(
                                    label = label,
                                    selected = isSelected,
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
                    val visibleLatestBlogs = if (
                        uiState.selectedTag == null &&
                        latestBlogs.firstOrNull()?.id == searchedBlogs.firstOrNull()?.id
                    ) {
                        latestBlogs.drop(1)
                    } else {
                        latestBlogs
                    }
                    if (visibleLatestBlogs.isEmpty()) {
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
                        items(visibleLatestBlogs, key = { it.id }) { blog ->
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
private fun BlogTagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(VitalRadius.Pill)
    val background = if (selected) Ink900 else Color(0xFFE7F5EA)
    val foreground = if (selected) Color.White else Mint700
    val borderColor = if (selected) Ink900 else Color(0xFFD5ECD9)

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(Mint300)
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            label,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        BlogCover(
            blog = blog,
            modifier = Modifier.fillMaxSize(),
            radius = 24.dp,
            fallbackGradient = true
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0.72f)
                    )
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = RoundedCornerShape(VitalRadius.Pill),
            color = Mint500
        ) {
            Text(
                "FEATURED",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(VitalRadius.Pill),
            color = Color.Black.copy(alpha = 0.22f)
        ) {
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = "Lưu bài",
                tint = Color.White,
                modifier = Modifier.padding(8.dp).size(22.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Text(
                "${blog.primaryTagLabel()} • ${blog.readMinutes()} min read",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                blog.title,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 25.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BlogListItem(blog: BlogDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        shadowElevation = VitalElevation.Level1
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlogCover(
                blog = blog,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(VitalRadius.Lg)),
                radius = VitalRadius.Lg,
                fallbackGradient = true
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .heightIn(min = 112.dp)
                    .weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(VitalRadius.Pill),
                    color = Mint100
                ) {
                    Text(
                        blog.primaryTagLabel(),
                        color = Mint700,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    blog.title,
                    color = Ink900,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Ink700, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("${blog.readMinutes()} min", color = Ink500, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun BlogDto.primaryTagLabel(): String = tags?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Nutrition"

private fun BlogDto.readMinutes(): Int {
    val body = content
        ?: blocks
            ?.sortedBy { it.order }
            ?.mapNotNull { it.textContent }
            ?.joinToString(" ")
    val wordCount = body
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.count { it.isNotBlank() }
        ?: 0
    return if (wordCount > 0) {
        (wordCount / 180).coerceAtLeast(1)
    } else {
        (title.length / 16).coerceIn(3, 8)
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
