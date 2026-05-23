package com.vitalai.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.components.VitalPill
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
                title = {
                    Column {
                        Text("Khám phá", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ink900)
                        Text("Bài viết sức khỏe mới nhất", fontSize = 12.sp, color = Ink500)
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.MyBlogs) }) {
                        Text("Bài của tôi", color = Ink700, fontSize = 13.sp)
                    }
                    VitalIconButton(
                        onClick = { navController.navigate(Screen.BlogComposer) },
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppMutedBackground)
                    .padding(vertical = 10.dp)
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
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                    item {
                        val featured = uiState.blogs.first()
                        SectionHeader(
                            title = "Nổi bật",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        FeaturedBlogCard(
                            blog = featured,
                            onClick = { navController.navigate(Screen.BlogDetail(featured.id)) }
                        )
                    }
                    if (uiState.blogs.drop(1).isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Bài viết mới",
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp)
                            )
                        }
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
}

@Composable
private fun FeaturedBlogCard(blog: BlogDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(232.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(VitalElevation.Level1)
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
                    .padding(18.dp)
            ) {
                blog.firstTag?.let {
                    Surface(
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Mint500.copy(alpha = 0.9f)
                    ) {
                        Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    blog.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                BlogMetaRow(blog = blog, inverse = true)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlogStatPill(icon = Icons.Default.Visibility, value = "${blog.viewCount}", inverse = true)
                BlogStatPill(icon = Icons.Default.Favorite, value = "${blog.likesCount}", inverse = true)
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
            if (blog.thumbnailUrl != null) {
                AsyncImage(
                    model = blog.thumbnailUrl,
                    contentDescription = blog.title,
                    modifier = Modifier
                        .size(width = 96.dp, height = 108.dp)
                        .clip(RoundedCornerShape(VitalRadius.Md)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 108.dp)
                        .clip(RoundedCornerShape(VitalRadius.Md))
                        .background(Mint50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = Mint600, modifier = Modifier.size(30.dp))
                }
            }
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
                BlogMetaRow(blog = blog, inverse = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlogStatPill(icon = Icons.Default.Visibility, value = "${blog.viewCount}")
                    BlogStatPill(icon = Icons.Default.Favorite, value = "${blog.likesCount}", color = MacroProtein)
                }
            }
        }
    }
}

@Composable
private fun BlogMetaRow(blog: BlogDto, inverse: Boolean) {
    val textColor = if (inverse) Color.White.copy(alpha = 0.78f) else Ink500
    Text(
        text = "${blog.displayAuthor} · ${blog.createdAt.take(10)}",
        fontSize = 12.sp,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun BlogStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    inverse: Boolean = false,
    color: Color = Mint500
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(if (inverse) Color.Black.copy(alpha = 0.34f) else AppSurface2)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (inverse) Color.White else color, modifier = Modifier.size(13.dp))
        Text(value, color = if (inverse) Color.White else Ink700, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
