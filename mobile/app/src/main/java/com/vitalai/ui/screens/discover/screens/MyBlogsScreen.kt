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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.screens.discover.components.BlogCover
import com.vitalai.ui.screens.discover.viewmodels.MyBlogsViewModel
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppMutedBackground
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink200
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.Mint700
import com.vitalai.ui.theme.VitalElevation
import com.vitalai.ui.theme.VitalRadius

private data class MyBlogTab(val status: String?, val label: String)

private val myBlogTabs = listOf(
    MyBlogTab(null, "Tất cả"),
    MyBlogTab("approved", "Đã duyệt"),
    MyBlogTab("pending", "Chờ duyệt"),
    MyBlogTab("draft", "Nháp"),
    MyBlogTab("rejected", "Bị từ chối")
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
                title = {
                    Text("Bài viết của tôi", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink900)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(52.dp)
                            .clickable { navController.navigate(Screen.BlogComposer()) },
                        shape = RoundedCornerShape(VitalRadius.Pill),
                        color = Color(0xFF19C287)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Tạo bài viết", tint = Color.White, modifier = Modifier.size(27.dp))
                        }
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
            MyBlogStatsRow(
                approved = uiState.approvedCount,
                views = uiState.totalViews,
                likes = uiState.totalLikes
            )
            MyBlogFilterTabs(
                selectedStatus = uiState.statusFilter,
                totalCount = uiState.allBlogs.size,
                counts = uiState.statusCounts,
                onSelect = viewModel::setStatusFilter
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Mint500)
                }
                uiState.filteredBlogs.isEmpty() -> EmptyMyBlogs(
                    message = uiState.error ?: "Chưa có bài viết trong mục này",
                    onCreate = { navController.navigate(Screen.BlogComposer()) }
                )
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                    items(uiState.filteredBlogs, key = { it.id }) { blog ->
                        MyBlogCard(
                            blog = blog,
                            onOpen = {
                                if (blog.status == "approved") navController.navigate(Screen.BlogDetail(blog.id))
                                else navController.navigate(Screen.BlogComposer(blog.id))
                            },
                            onEdit = { navController.navigate(Screen.BlogComposer(blog.id)) },
                            onDelete = { viewModel.deleteBlog(blog.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyBlogStatsRow(approved: Int, views: Int, likes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard("Đã duyệt", "$approved", Color(0xFF059669), Modifier.weight(1f))
        SummaryCard("Lượt xem", views.compactCount(), Ink900, Modifier.weight(1f))
        SummaryCard("Lượt thích", "$likes", MacroProtein, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF1F5F2)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, color = color, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Ink500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MyBlogFilterTabs(
    selectedStatus: String?,
    totalCount: Int,
    counts: Map<String, Int>,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        items(myBlogTabs) { tab ->
            val count = tab.status?.let { counts[it] ?: 0 } ?: totalCount
            val selected = selectedStatus == tab.status
            MyBlogFilterChip(
                label = tab.label,
                count = count,
                selected = selected,
                onClick = { onSelect(tab.status) }
            )
        }
    }
}

@Composable
private fun MyBlogFilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(if (selected) Color(0xFF062E22) else AppSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = if (selected) Color.White else Ink700, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(VitalRadius.Pill))
                .background(if (selected) Color.White.copy(alpha = 0.18f) else Color(0xFFEFF4F1))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$count", color = if (selected) Color.White else Ink500, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun MyBlogCard(blog: BlogDto, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(VitalElevation.Level1)
    ) {
        Column {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                BlogCover(
                    blog = blog,
                    modifier = Modifier.size(width = 112.dp, height = 112.dp),
                    radius = 14.dp,
                    fallbackGradient = true
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(blog.status)
                        if (blog.status == "draft") {
                            Spacer(Modifier.width(6.dp))
                            Text("· 60%", color = Ink500, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        blog.title,
                        color = Ink900,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(blog.displayDate(), color = Ink500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (blog.status == "approved") {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Metric(Icons.AutoMirrored.Filled.TrendingUp, blog.viewCount.compactCount())
                            Metric(Icons.Default.FavoriteBorder, "${blog.likesCount}")
                            Metric(Icons.AutoMirrored.Filled.MenuBook, "${blog.commentCount}")
                        }
                    }
                }
            }
            RejectionReason(blog)
            HorizontalDivider(color = Ink200, thickness = 0.7.dp)
            ActionRow(blog = blog, onOpen = onOpen, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    val config = when (status) {
        "approved" -> Triple("Đã duyệt", Color(0xFFD2F8E6), Color(0xFF047857))
        "pending" -> Triple("Chờ duyệt", Color(0xFFFFF1C9), Color(0xFFA15C00))
        "rejected" -> Triple("Bị từ chối", Color(0xFFFFDDE2), MacroProtein)
        "draft" -> Triple("Nháp", Color(0xFFEFF4F1), Ink700)
        else -> Triple("Nháp", Color(0xFFEFF4F1), Ink700)
    }
    Surface(shape = RoundedCornerShape(VitalRadius.Pill), color = config.second) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(config.third)
            )
            Spacer(Modifier.width(5.dp))
            Text(config.first, color = config.third, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun Metric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, tint = Ink500, modifier = Modifier.size(14.dp))
        Text(value, color = Ink500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RejectionReason(blog: BlogDto) {
    if (blog.status != "rejected" || blog.rejectionReason.isNullOrBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFDEE1))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MacroProtein, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("LÝ DO TỪ CHỐI", color = MacroProtein, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(blog.rejectionReason, color = Color(0xFF9F252B), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun ActionRow(blog: BlogDto, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        when (blog.status) {
            "approved" -> {
                FooterAction("Thống kê", Icons.Default.BarChart, Ink700, Modifier.weight(1f), onClick = onOpen)
                FooterDivider()
                FooterAction("Sửa", Icons.Default.Edit, Ink700, Modifier.weight(1f), onClick = onEdit)
                FooterDivider()
                FooterAction("Chia sẻ", Icons.Default.Share, Ink700, Modifier.weight(1f), onClick = onOpen)
            }
            "pending" -> {
                FooterAction("Sửa", Icons.Default.Edit, Ink700, Modifier.weight(1f), onClick = onEdit)
                FooterDivider()
                FooterAction("Hủy gửi", Icons.Default.Close, MacroProtein, Modifier.weight(1f), onClick = onDelete)
            }
            "rejected" -> {
                FooterAction("Sửa & gửi lại", Icons.Default.Edit, Color(0xFF059669), Modifier.weight(1f), onClick = onEdit)
                FooterDivider()
                FooterAction("Xóa", Icons.Default.Close, MacroProtein, Modifier.weight(1f), onClick = onDelete)
            }
            else -> {
                FooterAction("Tiếp tục viết", Icons.Default.Edit, Ink700, Modifier.weight(1f), onClick = onEdit)
                FooterDivider()
                FooterAction("Đăng bài", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF059669), Modifier.weight(1f), onClick = onEdit)
            }
        }
    }
}

@Composable
private fun FooterAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FooterDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .background(Ink200)
    )
}

@Composable
private fun EmptyMyBlogs(message: String, onCreate: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📝", fontSize = 44.sp)
            Spacer(Modifier.height(10.dp))
            Text(message, color = Ink500, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.clickable(onClick = onCreate),
                shape = RoundedCornerShape(VitalRadius.Pill),
                color = Color(0xFF19C287)
            ) {
                Text("Viết bài ngay", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

private fun Int.compactCount(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fm", this / 1_000_000f).removeSuffix(".0m") + "m"
        this >= 1_000 -> String.format("%.1fk", this / 1_000f).removeSuffix(".0k") + "k"
        else -> toString()
    }
}

private fun BlogDto.displayDate(): String {
    val date = createdAt.take(10)
    if (date.length != 10) return date
    val parts = date.split("-")
    if (parts.size != 3) return date
    val month = when (parts[1]) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
        else -> parts[1]
    }
    return "${parts[2]} $month ${parts[0]}"
}
