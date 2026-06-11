package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import com.vitalai.navigation.BottomNavReselectBus
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import androidx.compose.ui.platform.LocalDensity
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.components.VitalMainHeader
import com.vitalai.ui.components.VitalSmallHeader
import com.vitalai.ui.screens.discover.components.BlogCover
import com.vitalai.ui.screens.discover.components.BlogSearchBar
import com.vitalai.ui.screens.discover.components.formatBlogTime
import com.vitalai.ui.screens.discover.components.mergeBlogTags
import com.vitalai.ui.screens.discover.viewmodels.DiscoverViewModel
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    viewModel: DiscoverViewModel = hiltViewModel(),
    showBackButton: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val maxOffsetPx = with(LocalDensity.current) { 100.dp.toPx() }
    val scrollProgressProvider = remember {
        {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset / maxOffsetPx).coerceIn(0f, 1f)
            } else 1f
        }
    }
    val headerActions: @Composable RowScope.() -> Unit = {
        if (showBackButton) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
            }
        }
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
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh
    )

    var smallHeaderHeightPx by remember { mutableFloatStateOf(100f) }
    val initialSearchBarHeight = with(LocalDensity.current) { 80.dp.toPx() }
    var searchBarHeightPx by remember { mutableFloatStateOf(initialSearchBarHeight) }
    var mainHeaderHeightPx by remember { mutableFloatStateOf(200f) }

    val mainHeaderBottomY = remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                mainHeaderHeightPx - listState.firstVisibleItemScrollOffset
            } else {
                0f
            }
        }
    }

    var isScrollingUp by remember { mutableStateOf(false) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y > 5f) {
                    isScrollingUp = true
                } else if (available.y < -5f) {
                    isScrollingUp = false
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    var showQuickReturnState by remember { mutableStateOf(true) }
    LaunchedEffect(isScrollingUp, listState.isScrollInProgress) {
        if (!isScrollingUp) {
            showQuickReturnState = false
        } else if (!listState.isScrollInProgress) {
            showQuickReturnState = true
        }
    }

    val targetSearchBarOffset = remember(showQuickReturnState, mainHeaderBottomY.value) {
        if (mainHeaderBottomY.value > smallHeaderHeightPx) {
            0f
        } else if (showQuickReturnState) {
            0f
        } else {
            -searchBarHeightPx
        }
    }

    val animatedSearchBarOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetSearchBarOffset,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "searchBarOffset"
    )

    LaunchedEffect(Unit) {
        BottomNavReselectBus.events.collect { routeName ->
            if (routeName == Screen.Discover::class.qualifiedName) {
                listState.animateScrollToItem(0)
                viewModel.refresh()
            }
        }
    }
    val tags = remember(uiState.tags) {
        val mergedTags = mergeBlogTags(uiState.tags)
        listOf(null to "Tất cả") + mergedTags.map { it to it }
    }
    val searchedBlogs = uiState.blogs
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
    val recommendedBlogs = remember(searchedBlogs) { searchedBlogs.take(5) }
    val recommendedBlogIds = remember(recommendedBlogs) { recommendedBlogs.map { it.id }.toSet() }
    val recommendedPagerState = rememberPagerState(pageCount = { recommendedBlogs.size.coerceAtLeast(1) })

    Scaffold(
        containerColor = AppMutedBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .nestedScroll(nestedScrollConnection)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 0.dp, bottom = 28.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = AppSurface)
                    ) {
                        Box(modifier = Modifier.onGloballyPositioned { mainHeaderHeightPx = it.size.height.toFloat() }) {
                            VitalMainHeader(
                                title = "Khám phá",
                                textAlphaProvider = { 1f - scrollProgressProvider() },
                                actions = headerActions
                            )
                        }
                        Spacer(modifier = Modifier.height(with(LocalDensity.current) { searchBarHeightPx.toDp() }))
                    }
                }

                when {
                    uiState.isLoading -> item { LoadingState() }
                    uiState.error != null -> item { ErrorState(message = uiState.error!!, onRetry = viewModel::loadBlogs) }
                    searchedBlogs.isEmpty() -> item {
                        Box(
                            Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📰", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Chưa có bài viết nào", color = Ink500)
                            }
                        }
                    }
                    else -> {
                        item {
                            Text(
                                "Đề xuất",
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink900
                        )
                    }

                    item {
                        Column {
                            HorizontalPager(
                                state = recommendedPagerState,
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                pageSpacing = 14.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val blog = recommendedBlogs[page]
                                FeaturedBlogCard(
                                    blog = blog,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { navController.navigate(Screen.BlogDetail(blog.id)) }
                                )
                            }
                            RecommendPagerDots(
                                count = recommendedBlogs.size,
                                selectedIndex = recommendedPagerState.currentPage,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
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
                        uiState.selectedTag == null
                    ) {
                        latestBlogs
                            .filterNot { it.id in recommendedBlogIds }
                            .takeIf { it.isNotEmpty() }
                            ?: latestBlogs
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
            Box(
                modifier = Modifier
                    .offset { 
                        IntOffset(0, maxOf(smallHeaderHeightPx + animatedSearchBarOffset, mainHeaderBottomY.value).roundToInt())
                    }
                    .onGloballyPositioned { searchBarHeightPx = it.size.height.toFloat() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSurface)
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                        .clickable { navController.navigate(Screen.BlogSearch) }
                ) {
                    BlogSearchBar(
                        value = "",
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                contentColor = Mint500
            )

            VitalSmallHeader(
                title = "Khám phá",
                textAlphaProvider = scrollProgressProvider,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { smallHeaderHeightPx = it.size.height.toFloat() },
                actions = headerActions
            )
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
private fun RecommendPagerDots(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (index == selectedIndex) 18.dp else 7.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(if (index == selectedIndex) Mint600 else Mint100)
            )
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
private fun FeaturedBlogCard(blog: BlogDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
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
                blog.primaryTagLabel() ?: "Bài viết",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Text(
                formatBlogTime(blog.createdAt),
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
                blog.primaryTagLabel()?.let { tag ->
                    Surface(
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Mint100
                    ) {
                        Text(
                            tag,
                            color = Mint700,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
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
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Ink500, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(formatBlogTime(blog.createdAt), color = Ink500, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun BlogDto.primaryTagLabel(): String? = tags?.firstOrNull()?.takeIf { it.isNotBlank() }

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
