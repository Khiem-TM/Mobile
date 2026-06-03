package com.vitalai.ui.screens.discover.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import com.vitalai.ui.screens.discover.components.BlogCover
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
        containerColor = AppMutedBackground,
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
            uiState.query.isBlank() -> EmptySearchState(
                title = "Tìm kiếm bài viết",
                subtitle = "Nhập tiêu đề, tác giả hoặc tag để tìm bài viết.",
                modifier = Modifier.padding(padding)
            )
            uiState.results.isEmpty() -> EmptySearchState(
                title = "Không tìm thấy bài viết",
                subtitle = "Thử từ khóa khác hoặc kiểm tra lại dấu tiếng Việt.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${uiState.results.size} kết quả",
                        color = Ink500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.results, key = { it.id }) { blog ->
                    SearchResultCard(
                        blog = blog,
                        onClick = { navController.navigate(Screen.BlogDetail(blog.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(AppSurface2)
            .padding(horizontal = 14.dp),
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SearchResultCard(blog: BlogDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        shadowElevation = VitalElevation.Level1
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BlogCover(
                blog = blog,
                modifier = Modifier.size(82.dp),
                radius = VitalRadius.Lg,
                fallbackGradient = true
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                blog.firstTag?.let {
                    Text(it, color = Mint500, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    blog.title,
                    color = Ink900,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Ink500, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(blog.displayAuthor, color = Ink500, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
