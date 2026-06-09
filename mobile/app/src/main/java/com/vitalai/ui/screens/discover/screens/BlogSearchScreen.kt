package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.screens.discover.components.BlogCover
import com.vitalai.ui.screens.discover.components.formatBlogTime
import com.vitalai.ui.screens.discover.viewmodels.BlogSearchDateFilter
import com.vitalai.ui.screens.discover.viewmodels.BlogSearchSortOption
import com.vitalai.ui.screens.discover.viewmodels.BlogSearchViewModel
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppMutedBackground
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalElevation
import com.vitalai.ui.theme.VitalRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogSearchScreen(
    navController: NavController,
    viewModel: BlogSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = FocusRequester()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = AppSurface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
                    }
                },
                title = {
                    SearchField(
                        value = uiState.query,
                        onValueChange = viewModel::setQuery,
                        onSearch = viewModel::submitSearch,
                        onClear = viewModel::clearQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = viewModel::loadBlogs,
                modifier = Modifier.padding(padding)
            )
            uiState.submittedQuery.isBlank() -> SearchDiscoveryState(
                query = uiState.query,
                suggestions = uiState.suggestions,
                popularSearches = uiState.popularSearches,
                history = uiState.history,
                onSelect = viewModel::selectHistory,
                onRemove = viewModel::removeHistory,
                onClearAll = viewModel::clearHistory,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    SearchResultControls(
                        resultCount = uiState.results.size,
                        sortOption = uiState.sortOption,
                        dateFilter = uiState.dateFilter,
                        selectedTag = uiState.selectedTag,
                        availableTags = uiState.availableTags,
                        activeFilterCount = uiState.activeFilterCount,
                        onSortChange = viewModel::setSortOption,
                        onDateFilterChange = viewModel::setDateFilter,
                        onTagFilterChange = viewModel::setTagFilter,
                        onClearFilters = viewModel::clearFilters
                    )
                }
                item {
                    Spacer(Modifier.height(10.dp))
                }
                if (uiState.results.isEmpty()) {
                    item {
                        EmptySearchInlineState(
                            title = "Không tìm thấy bài viết",
                            subtitle = "Thử từ khóa khác hoặc chọn lại bộ lọc."
                        )
                    }
                } else {
                    items(uiState.results, key = { it.id }) { blog ->
                        SearchResultCard(
                            blog = blog,
                            onClick = {
                                viewModel.submitSearch()
                                navController.navigate(Screen.BlogDetail(blog.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(AppSurface2)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Ink400, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isBlank()) {
                Text("Tìm kiếm bài viết...", fontSize = 13.sp, color = Ink400)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Ink700),
                cursorBrush = SolidColor(Mint500),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "Xóa tìm kiếm",
                tint = Ink700,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .clickable(onClick = onClear)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun SearchResultCard(blog: BlogDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BlogCover(
            blog = blog,
            modifier = Modifier.size(58.dp),
            radius = 14.dp,
            fallbackGradient = true
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                blog.title,
                color = Ink900,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(VitalRadius.Pill))
                        .background(Mint500)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    listOfNotNull(blog.firstTag, blog.displayAuthor, formatBlogTime(blog.createdAt)).joinToString(" · "),
                    color = Ink500,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Ink500, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SearchResultControls(
    resultCount: Int,
    sortOption: BlogSearchSortOption,
    dateFilter: BlogSearchDateFilter?,
    selectedTag: String?,
    availableTags: List<String>,
    activeFilterCount: Int,
    onSortChange: (BlogSearchSortOption) -> Unit,
    onDateFilterChange: (BlogSearchDateFilter?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$resultCount bài viết tìm thấy",
            color = Ink500,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        ResultIconButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            selected = sortOption != BlogSearchSortOption.LATEST,
            contentDescription = "Sắp xếp",
            onClick = { sortExpanded = true }
        )
        Spacer(Modifier.width(8.dp))
        ResultIconButton(
            icon = Icons.Default.FilterList,
            selected = activeFilterCount > 0,
            contentDescription = "Bộ lọc",
            onClick = { filterExpanded = true }
        )
    }

    if (sortExpanded) {
        SortCenterDialog(
            sortOption = sortOption,
            onDismiss = { sortExpanded = false },
            onSelect = {
                onSortChange(it)
                sortExpanded = false
            }
        )
    }

    if (filterExpanded) {
        FilterCenterDialog(
            dateFilter = dateFilter,
            selectedTag = selectedTag,
            availableTags = availableTags,
            onDismiss = { filterExpanded = false },
            onDateSelect = {
                onDateFilterChange(it)
            },
            onTagSelect = {
                onTagFilterChange(it)
            },
            onClearFilters = onClearFilters
        )
    }
}

@Composable
private fun ResultIconButton(
    icon: ImageVector,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(if (selected) AppSurface2 else AppSurface)
            .border(1.dp, if (selected) Mint500 else AppLine, RoundedCornerShape(VitalRadius.Pill))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (selected) Mint500 else Ink700, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SortCenterDialog(
    sortOption: BlogSearchSortOption,
    onDismiss: () -> Unit,
    onSelect: (BlogSearchSortOption) -> Unit
) {
    var pendingSortOption by remember(sortOption) { mutableStateOf(sortOption) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sắp xếp bài viết", color = Ink900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BlogSearchSortOption.entries.forEach { option ->
                    DialogOptionRow(
                        label = option.label,
                        selected = option == pendingSortOption,
                        onClick = { pendingSortOption = option }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(pendingSortOption) }) {
                Text("Áp dụng", color = Mint500, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = Ink500, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun FilterCenterDialog(
    dateFilter: BlogSearchDateFilter?,
    selectedTag: String?,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onDateSelect: (BlogSearchDateFilter?) -> Unit,
    onTagSelect: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bộ lọc bài viết", color = Ink900, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Lọc theo ngày", color = Ink900, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                item {
                    DialogOptionRow(
                        label = "Tất cả thời gian",
                        selected = dateFilter == null,
                        onClick = { onDateSelect(null) }
                    )
                }
                items(BlogSearchDateFilter.entries, key = { it.name }) { filter ->
                    DialogOptionRow(
                        label = filter.label,
                        selected = filter == dateFilter,
                        onClick = { onDateSelect(filter) }
                    )
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("Lọc theo tag", color = Ink900, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                item {
                    DialogOptionRow(
                        label = "Tất cả tag",
                        selected = selectedTag == null,
                        onClick = { onTagSelect(null) }
                    )
                }
                items(availableTags, key = { it }) { tag ->
                    DialogOptionRow(
                        label = tag,
                        selected = tag == selectedTag,
                        onClick = { onTagSelect(tag) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Áp dụng", color = Mint500, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClearFilters) {
                Text("Xóa lọc", color = Ink500, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun DialogOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) AppSurface2 else AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Mint500 else AppLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (selected) Mint500 else Ink700,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Mint500, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SearchDiscoveryState(
    query: String,
    suggestions: List<String>,
    popularSearches: List<String>,
    history: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasQuery = query.trim().isNotBlank()
    var showAllHistory by remember { mutableStateOf(false) }
    val visibleHistory = if (showAllHistory) history else history.take(6)

    if (suggestions.isEmpty() && popularSearches.isEmpty() && history.isEmpty()) {
        EmptySearchState(
            title = "Tìm kiếm bài viết",
            subtitle = "Nhập tiêu đề, tác giả hoặc tag để tìm bài viết.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (hasQuery) {
            item {
                SearchListHeader(title = "Gợi ý tìm kiếm")
            }
            items(suggestions, key = { "suggestion-$it" }) { item ->
                CompactSearchRow(
                    query = item,
                    icon = Icons.Default.Search,
                    subtitle = "Tìm bài viết liên quan",
                    onSelect = { onSelect(item) },
                    onRemove = null
                )
            }
        } else {
            item {
                SearchListHeader(
                    title = "Mới đây",
                    action = if (history.size > 6 && !showAllHistory) "Xem tất cả" else if (history.isNotEmpty()) "Xóa tất cả" else null,
                    onAction = {
                        if (history.size > 6 && !showAllHistory) showAllHistory = true else onClearAll()
                    }
                )
            }

            if (visibleHistory.isEmpty()) {
                item {
                    Text(
                        "Chưa có tìm kiếm gần đây",
                        color = Ink500,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
                    )
                }
            } else {
                items(visibleHistory, key = { "history-$it" }) { item ->
                    CompactSearchRow(
                        query = item,
                        icon = Icons.Default.History,
                        subtitle = "Tìm kiếm gần đây",
                        onSelect = { onSelect(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }

            if (popularSearches.isNotEmpty()) {
                item {
                    SearchListHeader(
                        title = "Tìm kiếm phổ biến",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(popularSearches.take(6), key = { "popular-$it" }) { item ->
                    CompactSearchRow(
                        query = item,
                        icon = Icons.Default.Search,
                        subtitle = "Được tìm kiếm nhiều",
                        onSelect = { onSelect(item) },
                        onRemove = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchListHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Ink900,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                action,
                color = Mint500,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CompactSearchRow(
    query: String,
    icon: ImageVector,
    subtitle: String,
    onSelect: () -> Unit,
    onRemove: (() -> Unit)?
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 2.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = AppSurface2
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Mint500, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                query,
                color = Ink900,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(VitalRadius.Pill))
                        .background(Mint500)
                )
                Spacer(Modifier.width(6.dp))
                Text(subtitle, color = Ink500, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onRemove != null) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Tùy chọn", tint = Ink500, modifier = Modifier.size(22.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Xóa khỏi lịch sử", color = Ink700) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(title, color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Ink500, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptySearchInlineState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(title, color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Ink500, fontSize = 13.sp)
        }
    }
}
