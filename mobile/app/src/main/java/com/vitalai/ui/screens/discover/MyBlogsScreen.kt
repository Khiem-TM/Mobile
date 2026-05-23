package com.vitalai.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vitalai.ui.theme.*

private val statusTabs = listOf(
    null to "Tất cả",
    "approved" to "Đã duyệt",
    "pending" to "Chờ duyệt",
    "draft" to "Nháp",
    "rejected" to "Bị từ chối"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBlogsScreen(
    navController: NavController,
    viewModel: MyBlogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài viết của tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    FloatingActionButton(
                        onClick = { navController.navigate(Screen.BlogComposer) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp),
                        containerColor = Mint500,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo bài viết", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatsItem(label = "Đã duyệt", value = "${uiState.approvedCount}", color = Mint500)
                    VerticalDivider(modifier = Modifier.height(36.dp), color = Ink200)
                    StatsItem(label = "Lượt xem", value = "${uiState.totalViews}", color = MacroWater)
                    VerticalDivider(modifier = Modifier.height(36.dp), color = Ink200)
                    StatsItem(label = "Lượt thích", value = "${uiState.totalLikes}", color = MacroProtein)
                }
            }

            // Status tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurface)
                    .padding(vertical = 6.dp)
            ) {
                items(statusTabs) { (key, label) ->
                    val isSelected = uiState.statusFilter == key
                    val count = when (key) {
                        null -> uiState.allBlogs.size
                        else -> uiState.statusCounts[key] ?: 0
                    }
                    StatusTabChip(
                        label = label,
                        count = count,
                        isSelected = isSelected,
                        onClick = { viewModel.setStatusFilter(if (isSelected && key != null) null else key) }
                    )
                }
            }

            // Blog list
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Mint500)
                }
            } else if (uiState.filteredBlogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Chưa có bài viết nào", color = Ink500, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate(Screen.BlogComposer) },
                            colors = ButtonDefaults.buttonColors(containerColor = Mint500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Viết bài ngay")
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(uiState.filteredBlogs) { blog ->
                        MyBlogCard(
                            blog = blog,
                            onClick = { navController.navigate(Screen.BlogDetail(blog.id)) },
                            onEdit = { navController.navigate(Screen.BlogComposer) },
                            onDelete = { viewModel.deleteBlog(blog.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        Text(label, fontSize = 11.sp, color = Ink500)
    }
}

@Composable
private fun StatusTabChip(label: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(if (isSelected) Mint500 else AppSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Ink700,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(if (isSelected) Color.White.copy(0.25f) else Ink200)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    "$count",
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White else Ink700,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MyBlogCard(
    blog: BlogDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (blog.status) {
        "approved" -> Mint500
        "pending" -> MacroCarbs
        "rejected" -> MacroProtein
        else -> Ink300
    }
    val statusLabel = when (blog.status) {
        "approved" -> "Đã duyệt"
        "pending" -> "Chờ duyệt"
        "rejected" -> "Bị từ chối"
        else -> "Nháp"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            // Rejection reason banner
            if (blog.status == "rejected") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MacroProtein.copy(0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MacroProtein, modifier = Modifier.size(14.dp))
                    Text("Bài viết không đáp ứng tiêu chuẩn cộng đồng", fontSize = 11.sp, color = MacroProtein)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail
                if (blog.thumbnailUrl != null) {
                    AsyncImage(
                        model = blog.thumbnailUrl,
                        contentDescription = blog.title,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Mint50),
                        contentAlignment = Alignment.Center
                    ) { Text("📰", fontSize = 24.sp) }
                }

                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(statusColor.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(statusLabel, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        blog.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Ink900,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(blog.createdAt.take(10), fontSize = 11.sp, color = Ink500)

                    if (blog.status == "approved") {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Ink500, modifier = Modifier.size(12.dp))
                                Text("${blog.viewCount}", fontSize = 11.sp, color = Ink500)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MacroProtein, modifier = Modifier.size(12.dp))
                                Text("${blog.likesCount}", fontSize = 11.sp, color = Ink500)
                            }
                        }
                    }
                }
            }

            // Actions footer
            HorizontalDivider(color = Ink200, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                when (blog.status) {
                    "draft", "rejected" -> {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Chỉnh sửa", fontSize = 12.sp)
                        }
                    }
                    "approved" -> {
                        TextButton(onClick = onClick) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Xem bài", fontSize = 12.sp, color = Mint500)
                        }
                    }
                    else -> {}
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MacroProtein, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Xóa", fontSize = 12.sp, color = MacroProtein)
                }
            }
        }
    }
}
