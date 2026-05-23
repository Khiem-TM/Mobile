package com.vitalai.ui.screens.coach

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.ui.components.VitalBottomNavBar
import com.vitalai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    navController: NavController,
    viewModel: CoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val suggestions = listOf(
        "Thực đơn hôm nay 🥗",
        "Bài tập cho tôi 💪",
        "Phân tích dinh dưỡng 📊",
        "Mẹo giảm cân 🎯"
    )

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                sessions = uiState.sessions,
                currentSessionId = uiState.currentSessionId,
                onSelectSession = { sessionId ->
                    viewModel.selectSession(sessionId)
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = viewModel::deleteSession,
                onNewSession = {
                    viewModel.createNewSession()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Mint500, Mint700))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("V", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Vita · AI Coach", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink900)
                                Text("Đang hoạt động", fontSize = 11.sp, color = Mint500)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Lịch sử trò chuyện", tint = Ink700)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createNewSession() }) {
                            Icon(Icons.Default.Add, contentDescription = "Cuộc trò chuyện mới", tint = Ink700)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
                )
            },
            bottomBar = { VitalBottomNavBar(navController = navController) },
            containerColor = AppMutedBackground
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Welcome / empty state
                if (uiState.currentSessionId == null && !uiState.isLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✨", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Xin chào! Tôi là Vita", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink900)
                            Text("AI Coach dinh dưỡng & sức khoẻ của bạn", color = Ink500, fontSize = 14.sp)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.createNewSession() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Mint500)
                            ) {
                                Text("Bắt đầu trò chuyện")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages) { msg ->
                            ChatBubble(role = msg.role, content = msg.content)
                        }
                        if (uiState.isSending) {
                            item { TypingIndicator() }
                        }
                    }
                }

                // Quick suggestion chips (only when session is empty)
                if (uiState.messages.isEmpty() && uiState.currentSessionId != null) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(suggestions) { s ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.updateInput(s)
                                    viewModel.sendMessage()
                                },
                                label = { Text(s, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Mint50,
                                    labelColor = Mint700
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Mint200
                                )
                            )
                        }
                    }
                }

                // Input row
                Surface(color = AppSurface, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = viewModel::updateInput,
                            placeholder = { Text("Hỏi Vita điều gì đó...", color = Ink500, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Mint500,
                                unfocusedBorderColor = Ink200,
                                focusedContainerColor = AppSurface,
                                unfocusedContainerColor = AppSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val canSend = uiState.inputText.isNotBlank() && !uiState.isSending
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = canSend,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (canSend) Mint500 else Ink200)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Gửi",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── History Drawer ─────────────────────────────────────────────────────────────

private val DrawerBg = Color(0xFF111827)
private val DrawerItemBg = Color(0xFF1F2937)
private val DrawerItemSelected = Color(0xFF374151)
private val DrawerText = Color(0xFFF9FAFB)
private val DrawerSubText = Color(0xFF9CA3AF)
private val DrawerDivider = Color(0xFF374151)

@Composable
private fun ChatHistoryDrawer(
    sessions: List<ChatSessionDto>,
    currentSessionId: String?,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewSession: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DrawerBg,
        modifier = Modifier.fillMaxHeight()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Mint500, Mint700))),
                contentAlignment = Alignment.Center
            ) {
                Text("V", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text("AI Coach", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DrawerText)
        }

        // New chat button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DrawerItemBg)
                .clickable(onClick = onNewSession)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = DrawerText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Cuộc trò chuyện mới", color = DrawerText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = DrawerDivider, thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Chưa có cuộc hội thoại nào", color = DrawerSubText, fontSize = 13.sp)
            }
        } else {
            Text(
                "Lịch sử",
                color = DrawerSubText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sessions, key = { it.id }) { session ->
                    SessionHistoryItem(
                        session = session,
                        isSelected = session.id == currentSessionId,
                        onSelect = { onSelectSession(session.id) },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryItem(
    session: ChatSessionDto,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DrawerItemSelected else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title ?: "Cuộc trò chuyện",
                color = DrawerText,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!session.lastMessage.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = session.lastMessage,
                    color = DrawerSubText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Xóa",
                tint = DrawerSubText,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ── Chat UI components ─────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(role: String, content: String) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Mint500),
                contentAlignment = Alignment.Center
            ) {
                Text("V", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isUser) Ink900 else AppSurface,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                color = if (isUser) Color.White else Color(0xFF1F2937),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Mint500),
            contentAlignment = Alignment.Center
        ) {
            Text("V", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = AppSurface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = i * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Mint500.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
