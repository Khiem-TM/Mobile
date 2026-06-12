package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.graphics.toArgb
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import coil.compose.AsyncImage
import com.vitalai.core.network.ImageUrlResolver
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.BottomSheetGrabber
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    id: String,
    navController: NavController,
    detailViewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val detailState by detailViewModel.uiState.collectAsState()
    val exercise = detailState.exercise
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        detailViewModel.loadExercise(id)
    }

    LaunchedEffect(detailState.saveSuccess, detailState.error) {
        when {
            detailState.saveSuccess -> {
                snackbarHostState.showSnackbar("Đã thêm vào buổi tập")
                detailViewModel.clearSaveState()
                showAddSheet = false
            }
            detailState.error != null -> {
                snackbarHostState.showSnackbar(detailState.error ?: "Có lỗi xảy ra")
                detailViewModel.clearSaveState()
            }
        }
    }

    Scaffold(
        containerColor = TrainColors.Cream,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            ExerciseDetailScreenContent(
                exercise = exercise,
                isLoading = detailState.isLoading && exercise == null,
                errorMessage = detailState.error,
                isFavorite = exercise?.isFavorite == true,
                onRetry = { detailViewModel.loadExercise(id) },
                onBackClick = { navController.popBackStack() },
                onFavoriteToggle = detailViewModel::toggleFavorite,
                onAddToWorkoutClick = { showAddSheet = true }
            )
        }
    }

    if (showAddSheet && exercise != null) {
        AddToSessionSheet(
            exercise = exercise,
            isSaving = detailState.isSaving,
            onDismiss = { showAddSheet = false },
            onSubmit = detailViewModel::addToTodaySession
        )
    }
}

@Composable
fun ExerciseDetailScreenContent(
    exercise: ExerciseDto?,
    isLoading: Boolean,
    errorMessage: String?,
    isFavorite: Boolean,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToWorkoutClick: () -> Unit
) {
    when {
        isLoading -> ExerciseDetailSkeleton(onBackClick)
        exercise == null -> ErrorState(message = errorMessage ?: "Không tìm thấy bài tập", onRetry = onRetry)
        else -> DetailShell(
            exercise = exercise,
            isFavorite = isFavorite,
            onBackClick = onBackClick,
            onFavoriteToggle = onFavoriteToggle,
            onAddToWorkoutClick = onAddToWorkoutClick
        )
    }
}

@Composable
private fun DetailShell(
    exercise: ExerciseDto,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToWorkoutClick: () -> Unit
) {
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var videoDurationSeconds by remember(exercise.videoUrl) { mutableStateOf<Int?>(null) }
    val mediaItems = remember(exercise) { exerciseMediaItems(exercise) }
    Box(Modifier.fillMaxSize().background(TrainColors.Cream)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 108.dp)
        ) {
            item {
                ExerciseHeroMedia(
                    exercise = exercise,
                    mediaItems = mediaItems,
                    isFavorite = isFavorite,
                    onBackClick = onBackClick,
                    onFavoriteToggle = onFavoriteToggle,
                    videoDurationSeconds = videoDurationSeconds,
                    onOpenVideo = { videoUrl = it }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TrainColors.Cream)
                        .padding(start = 21.dp, end = 21.dp, top = 23.dp)
                ) {
                    Text(
                        exercise.name,
                        color = TrainColors.Forest,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = com.vitalai.ui.theme.VitalDisplayFontFamily
                    )
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        TrainTypeBadge(exercise.exerciseType)
                        Text(exercise.category ?: exercise.muscleGroup, color = TrainColors.Charcoal, fontSize = 14.sp)
                        Text("·", color = TrainColors.Charcoal.copy(alpha = 0.55f), fontSize = 14.sp)
                        Text("★ ${exerciseDifficultyLabel(exercise.difficultyLevel)}", color = TrainColors.Sport, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = TrainColors.Cardio, modifier = Modifier.size(16.dp))
                        Text("${exercise.favoritesCount} người yêu thích", color = TrainColors.Charcoal, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(26.dp))
                    ExercisePixelStats(exercise)
                    Spacer(Modifier.height(23.dp))
                    ExerciseMuscleAndEquipment(exercise)
                    Spacer(Modifier.height(20.dp))
                    TextBlocks(exercise)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(TrainColors.Cream)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 13.dp, bottom = 13.dp)
        ) {
            TrainPrimaryButton(
                text = "Thêm vào buổi tập hôm nay",
                icon = Icons.Default.Add,
                onClick = onAddToWorkoutClick,
                modifier = Modifier.height(56.dp)
            )
        }
    }

    videoUrl?.let { url ->
        ExerciseVideoDialog(
            url = url,
            onDurationAvailable = { videoDurationSeconds = it },
            onDismiss = { videoUrl = null }
        )
    }
}

@Composable
private fun ExerciseDetailSkeleton(onBackClick: () -> Unit) {
    Box(Modifier.fillMaxSize().background(TrainColors.Cream)) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Color(0xFFC8DCC8))
            )
            Column(
                Modifier.padding(horizontal = 21.dp, vertical = 23.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        Modifier
                            .fillMaxWidth(if (index == 0) 0.62f else 1f)
                            .height(if (index == 0) 34.dp else 72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TrainColors.KeylimeWash)
                    )
                }
            }
        }
        Box(
            Modifier
                .statusBarsPadding()
                .padding(start = 18.dp, top = 18.dp)
        ) {
            HeroCircleButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
        }
    }
}

private enum class ExerciseMediaType { Video, Image, Lottie }

private data class ExerciseMediaItem(
    val type: ExerciseMediaType,
    val url: String?,
    val previewUrl: String?,
    val lottieAsset: String? = null
)

private fun exerciseMediaItems(exercise: ExerciseDto): List<ExerciseMediaItem> {
    if (exercise.lottieAsset != null) {
        return listOf(ExerciseMediaItem(ExerciseMediaType.Lottie, null, null, exercise.lottieAsset))
    }
    val images = exercise.imageUrl.orEmpty()
        .mapNotNull { ImageUrlResolver.resolve(it) }
        .ifEmpty { listOfNotNull(exercise.displayImageUrl) }
        .distinct()
    val items = mutableListOf<ExerciseMediaItem>()
    if (!exercise.videoUrl.isNullOrBlank()) {
        items += ExerciseMediaItem(ExerciseMediaType.Video, exercise.videoUrl, images.firstOrNull())
    }
    images.forEach { items += ExerciseMediaItem(ExerciseMediaType.Image, it, it) }
    if (items.isEmpty()) items += ExerciseMediaItem(ExerciseMediaType.Image, null, null)
    return items
}

@Composable
private fun ExerciseHeroMedia(
    exercise: ExerciseDto,
    mediaItems: List<ExerciseMediaItem>,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    videoDurationSeconds: Int?,
    onOpenVideo: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val page by remember { derivedStateOf { listState.firstVisibleItemIndex.coerceAtMost(mediaItems.lastIndex) } }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFFC8DCC8))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            LazyRow(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(mediaItems) { _, item ->
                    ExerciseHeroPage(
                        exercise = exercise,
                        item = item,
                        width = maxWidth,
                        onOpenVideo = onOpenVideo
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroCircleButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeroCircleButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isFavorite) TrainColors.Cardio else TrainColors.Forest,
                    onClick = onFavoriteToggle
                )
                HeroCircleButton(icon = Icons.Default.MoreHoriz, onClick = {})
            }
        }

        if (mediaItems[page].type == ExerciseMediaType.Video && !mediaItems[page].url.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TrainColors.Forest.copy(alpha = 0.62f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TrainColors.Cream, modifier = Modifier.size(14.dp))
                Text(
                    "${videoDurationSeconds?.let(::formatVideoDuration) ?: "Video"} · HD",
                    color = TrainColors.Cream,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 17.dp, bottom = 19.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mediaItems.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .size(width = if (index == page) 21.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(TrainColors.Cream.copy(alpha = if (index == page) 0.95f else 0.55f))
                )
            }
        }
    }
}

@Composable
private fun ExerciseHeroPage(
    exercise: ExerciseDto,
    item: ExerciseMediaItem,
    width: androidx.compose.ui.unit.Dp,
    onOpenVideo: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(Color(0xFFC8DCC8)),
        contentAlignment = Alignment.Center
    ) {
        if (item.type == ExerciseMediaType.Lottie && item.lottieAsset != null) {
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset(item.lottieAsset))
            val dynamicProperties = rememberLottieDynamicProperties(
                rememberLottieDynamicProperty(
                    property = LottieProperty.COLOR,
                    value = TrainColors.Cream.toArgb(),
                    keyPath = arrayOf("**", "White Solid 1")
                )
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                contentScale = ContentScale.Crop,
                dynamicProperties = dynamicProperties,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!item.previewUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.previewUrl,
                contentDescription = exercise.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TrainColors.Forest.copy(alpha = 0.35f), modifier = Modifier.size(104.dp))
        }
        if (item.type == ExerciseMediaType.Video && !item.url.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(TrainColors.Cream.copy(alpha = 0.94f))
                    .clickable { onOpenVideo(item.url) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Phát video", tint = TrainColors.Forest, modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun HeroCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = TrainColors.Forest,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(TrainColors.Cream.copy(alpha = 0.96f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun ExercisePixelStats(exercise: ExerciseDto) {
    Column {
        Text("Thông số mặc định", color = TrainColors.Charcoal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        when (exercise.exerciseType.uppercase()) {
            "GYM" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DefaultStat((exercise.defaultSets ?: 4).toString(), "Sets", Modifier.weight(1f), TrainTone.Keylime)
                DefaultStat((exercise.defaultReps ?: 6).toString(), "Reps", Modifier.weight(1f), TrainTone.Keylime)
                DefaultStat(defaultWeightText(exercise), "Tạ", Modifier.weight(1f), TrainTone.Keylime)
            }
            "CARDIO" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DefaultStat("${exercise.defaultDurationMinutes ?: 30}", "Phút", Modifier.weight(1f), TrainTone.Keylime)
                DefaultStat("%.1f".format(exercise.metValue.takeIf { it > 0f } ?: 6f), "MET", Modifier.weight(1f), TrainTone.Keylime)
                DefaultStat("${(exercise.estimatedCaloriesPerMinute ?: exercise.caloriesPerMin).roundToInt()}", "kcal/phút", Modifier.weight(1f), TrainTone.Keylime)
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DefaultStat("${exercise.defaultDurationMinutes ?: 30}", "Phút", Modifier.weight(1f), TrainTone.Keylime)
                DefaultStat(intensityLabel(exercise.defaultIntensityLevel ?: "MEDIUM"), "Cường độ", Modifier.weight(1f), TrainTone.Keylime)
                DefaultStat("%.1f".format(exercise.metValue.takeIf { it > 0f } ?: 6f), "MET", Modifier.weight(1f), TrainTone.Keylime)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("◷", color = TrainColors.Forest, fontSize = 17.sp)
            Text(
                when (exercise.exerciseType.uppercase()) {
                    "GYM" -> "Nghỉ giữa set: "
                    "CARDIO" -> "Pace/MET: "
                    else -> "Cường độ mặc định: "
                },
                color = TrainColors.Charcoal,
                fontSize = 14.sp
            )
            Text(
                when (exercise.exerciseType.uppercase()) {
                    "GYM" -> "${exercise.restTimeSeconds ?: 150} giây · MET: %.1f".format(exercise.metValue.takeIf { it > 0f } ?: 6f)
                    "CARDIO" -> "MET %.1f".format(exercise.metValue.takeIf { it > 0f } ?: 6f)
                    else -> intensityLabel(exercise.defaultIntensityLevel ?: "MEDIUM")
                },
                color = TrainColors.Forest,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExerciseMuscleAndEquipment(exercise: ExerciseDto) {
    val muscles = buildList {
        exercise.targetMuscleGroup?.takeIf { it.isNotBlank() }?.let { add(it) }
        exercise.muscleGroup.takeIf { it.isNotBlank() }?.let { add(it) }
        addAll(exercise.secondaryMuscleGroups.orEmpty())
    }.distinct().ifEmpty { listOf(exercise.category ?: "Toàn thân") }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text("Nhóm cơ mục tiêu", color = TrainColors.Charcoal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                muscles.take(4).forEach { muscle ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TrainColors.MintKiss)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(muscleLabel(muscle), color = TrainColors.Forest, fontSize = 14.sp)
                    }
                }
            }
        }
        Column {
            Text("Dụng cụ", color = TrainColors.Charcoal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("↱", color = TrainColors.Forest, fontSize = 17.sp)
                Text(equipmentLabel(exercise.equipment), color = TrainColors.Charcoal, fontSize = 14.sp)
            }
        }
        Column {
            Text("Mô tả", color = TrainColors.Charcoal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                exercise.description ?: "Chưa có mô tả",
                color = TrainColors.Charcoal,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun ExerciseVideoDialog(
    url: String,
    onDurationAvailable: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentOnDurationAvailable by rememberUpdatedState(onDurationAvailable)
    val videoId = remember(url) { youtubeVideoId(url) }
    var retryKey by remember { mutableIntStateOf(0) }
    var isReady by remember(retryKey) { mutableStateOf(false) }
    var playerError by remember(retryKey) { mutableStateOf<String?>(null) }
    var playerErrorCode by remember(retryKey) { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    LaunchedEffect(retryKey, videoId) {
        delay(15_000)
        if (!isReady && playerError == null) {
            playerError = "Video tải quá lâu. Vui lòng kiểm tra kết nối và thử lại."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = TrainColors.Forest) }
        },
        title = { Text("Video hướng dẫn", color = TrainColors.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                key(retryKey) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply webView@{
                                setBackgroundColor(android.graphics.Color.BLACK)
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            playerError = "Không thể tải trình phát YouTube"
                                        }
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.loadsImagesAutomatically = true
                                CookieManager.getInstance().apply {
                                    setAcceptCookie(true)
                                    setAcceptThirdPartyCookies(this@webView, true)
                                }
                                if (videoId != null) {
                                    addJavascriptInterface(
                                        YouTubePlayerBridge(
                                            onReady = { isReady = true },
                                            onDurationAvailable = { currentOnDurationAvailable(it) },
                                            onError = {
                                                isReady = false
                                                playerErrorCode = it
                                                playerError = "Không thể phát video YouTube (mã lỗi $it)"
                                            }
                                        ),
                                        "VitalAI"
                                    )
                                    loadDataWithBaseURL(
                                        YOUTUBE_ORIGIN,
                                        youtubePlayerHtml(videoId),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                } else {
                                    playerError = "Link video không hợp lệ"
                                }
                            }
                        },
                        onRelease = { releasedWebView ->
                            releasedWebView.stopLoading()
                            releasedWebView.loadUrl("about:blank")
                            releasedWebView.removeJavascriptInterface("VitalAI")
                            releasedWebView.destroy()
                        }
                    )
                }

                if (!isReady && playerError == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = TrainColors.Cream
                    )
                }
                playerError?.let { message ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(message, color = Color.White, fontSize = 13.sp)
                        if (playerErrorCode in YOUTUBE_EMBED_RESTRICTED_ERRORS) {
                            Button(onClick = { openYouTubeVideo(context, url, videoId) }) {
                                Text("Mở trên YouTube")
                            }
                        } else {
                            Button(onClick = { retryKey += 1 }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
            }
        },
        containerColor = TrainColors.Cream
    )
}

private val YOUTUBE_EMBED_RESTRICTED_ERRORS = setOf(101, 150, 152)

private fun openYouTubeVideo(context: android.content.Context, url: String, videoId: String?) {
    if (videoId != null) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to the browser when the YouTube app is unavailable.
        }
    }
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun youtubeVideoId(url: String): String? {
    val trimmed = url.trim()
    val videoId = when {
        "youtu.be/" in trimmed -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
        "v=" in trimmed -> trimmed.substringAfter("v=").substringBefore("&")
        "/embed/" in trimmed -> trimmed.substringAfter("/embed/").substringBefore("?")
        "/shorts/" in trimmed -> trimmed.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
        else -> ""
    }
    return videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}

private const val YOUTUBE_ORIGIN = "https://www.youtube.com"

private fun youtubePlayerHtml(videoId: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body, #player { width: 100%; height: 100%; margin: 0; background: #000; }
        </style>
      </head>
      <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
          function reportDuration(player, retries) {
            var duration = Math.round(player.getDuration());
            if (duration > 0) {
              VitalAI.onDurationAvailable(duration);
            } else if (retries > 0) {
              setTimeout(function() { reportDuration(player, retries - 1); }, 500);
            }
          }

          function onYouTubeIframeAPIReady() {
            new YT.Player('player', {
              videoId: '$videoId',
              playerVars: {
                autoplay: 1,
                playsinline: 1,
                rel: 0,
                origin: '$YOUTUBE_ORIGIN'
              },
              events: {
                onReady: function(event) {
                  VitalAI.onPlayerReady();
                  reportDuration(event.target, 20);
                  event.target.playVideo();
                },
                onStateChange: function(event) {
                  reportDuration(event.target, 4);
                },
                onError: function(event) {
                  VitalAI.onPlayerError(event.data);
                }
              }
            });
          }
        </script>
      </body>
    </html>
""".trimIndent()

private class YouTubePlayerBridge(
    private val onReady: () -> Unit,
    private val onDurationAvailable: (Int) -> Unit,
    private val onError: (Int) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onDurationAvailable(durationSeconds: Int) {
        if (durationSeconds > 0) {
            mainHandler.post { onDurationAvailable(durationSeconds) }
        }
    }

    @JavascriptInterface
    fun onPlayerReady() {
        mainHandler.post(onReady)
    }

    @JavascriptInterface
    fun onPlayerError(errorCode: Int) {
        mainHandler.post { onError(errorCode) }
    }
}

private fun formatVideoDuration(durationSeconds: Int): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private fun defaultWeightText(exercise: ExerciseDto): String {
    val weight = exercise.defaultWeightKg ?: 100f
    return if (weight % 1f == 0f) "${weight.toInt()} kg" else "%.1f kg".format(weight)
}

private fun muscleLabel(value: String): String = when (value.uppercase()) {
    "LOWER BACK", "BACK", "LATS" -> "Lưng dưới"
    "GLUTES" -> "Mông"
    "HAMSTRINGS" -> "Đùi sau"
    "ARMS", "BICEPS", "TRICEPS" -> "Cánh tay"
    "CHEST" -> "Ngực"
    "SHOULDERS" -> "Vai"
    "LEGS", "QUADS" -> "Chân"
    else -> value
}

private fun equipmentLabel(value: String?): String = when (value?.lowercase()) {
    null, "", "none", "none (bodyweight exercise)", "bodyweight" -> "Trọng lượng cơ thể"
    "dumbbell" -> "Tạ đơn"
    "barbell" -> "Tạ đòn"
    "bench" -> "Ghế tập"
    else -> value
}

private fun exerciseDifficultyLabel(value: String?): String = when (value?.uppercase()) {
    "BEGINNER" -> "Dễ"
    "INTERMEDIATE" -> "Vừa"
    "ADVANCED" -> "Khó"
    else -> value ?: "Vừa"
}

@Composable
private fun maxLinesModifier(): Modifier = Modifier

@Composable
private fun GymDetailContent(exercise: ExerciseDto) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Thông số mặc định") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DefaultStat((exercise.defaultSets ?: 3).toString(), "Sets", Modifier.weight(1f))
                DefaultStat((exercise.defaultReps ?: 10).toString(), "Reps", Modifier.weight(1f))
                DefaultStat("${exercise.defaultWeightKg ?: 0f} kg", "Tạ", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            InfoRow("Nghỉ giữa set", "${exercise.restTimeSeconds ?: 90} giây")
            if (exercise.metValue > 0f) InfoRow("MET", "%.1f".format(exercise.metValue))
        }
        Section("Dụng cụ") {
            InfoRow("Thiết bị", exercise.equipment ?: "Không cần dụng cụ")
        }
    }
}

@Composable
private fun SportDetailContent(exercise: ExerciseDto) {
    val rates = listOf("LOW" to 4, "MEDIUM" to 7, "HIGH" to 10)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Ước tính tiêu hao calo") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rates.forEach { (key, rate) ->
                    DefaultStat(rate.toString(), "kcal/phút · ${intensityLabel(key)}", Modifier.weight(1f), tone = if (key == "MEDIUM") TrainTone.Mint else TrainTone.Keylime)
                }
            }
        }
        TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
            InfoRow("Thời lượng khuyến nghị", "${exercise.defaultDurationMinutes ?: 30} phút")
            InfoRow("Loại vận động", exercise.movementType ?: "Toàn thân")
            InfoRow("Cường độ mặc định", intensityLabel(exercise.defaultIntensityLevel ?: "MEDIUM"))
            if (exercise.metValue > 0f) InfoRow("MET", "%.1f".format(exercise.metValue))
        }
    }
}

@Composable
private fun CardioDetailContent(exercise: ExerciseDto) {
    var distance by remember { mutableFloatStateOf(5f) }
    var speed by remember { mutableFloatStateOf(10f) }
    val duration = cardioDuration(distance, speed)
    val kcal = cardioCalories(exercise, distance, speed)
    val met = if (exercise.metValue > 0f) exercise.metValue else runningMet(speed)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Mô phỏng nhanh") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainNumberStepper("Khoảng cách (km)", distance, { distance = it }, 0.5f, 0.5f, 1000f, Modifier.weight(1f), decimals = 1)
                TrainNumberStepper("Tốc độ (km/h)", speed, { speed = it }, 0.5f, 0.5f, 80f, Modifier.weight(1f), decimals = 1)
            }
            Spacer(Modifier.height(14.dp))
            TrainCard(tone = TrainTone.Mint, padding = PaddingValues(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    SimStat("${duration.roundToInt()}", "Thời gian", "phút")
                    SimStat(formatPace(speed), "Pace", "/km")
                    SimStat("~$kcal", "Ước tính", "kcal")
                }
            }
        }
        Section("Bảng MET theo tốc độ") {
            TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
                InfoRow("< 6 km/h", "MET 3.5")
                InfoRow("6-8 km/h", "MET 6.0")
                InfoRow("8-10 km/h", "MET 8.3")
                InfoRow("> 10 km/h", "MET 9.8+")
            }
            Text("Đang áp dụng MET %.1f cho tốc độ $speed km/h".format(met), color = TrainColors.Charcoal.copy(alpha = 0.7f), fontSize = 11.5.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        TrainSectionTitle(title, modifier = Modifier.padding(bottom = 11.dp))
        content()
    }
}

@Composable
private fun DefaultStat(value: String, label: String, modifier: Modifier = Modifier, tone: TrainTone = TrainTone.Keylime) {
    TrainCard(modifier = modifier, tone = tone, padding = PaddingValues(14.dp)) {
        Text(value, modifier = Modifier.fillMaxWidth(), color = TrainColors.Forest, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(label, modifier = Modifier.fillMaxWidth(), color = TrainColors.Charcoal, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TrainColors.Charcoal, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = TrainColors.Forest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SimStat(value: String, label: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TrainColors.Forest, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TrainColors.Charcoal, fontSize = 11.sp)
        Text(sub, color = TrainColors.Charcoal.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
private fun TextBlocks(exercise: ExerciseDto) {
    val instructions = splitExerciseText(exercise.instructions)
    val tips = splitExerciseText(exercise.formTips)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Cách thực hiện") {
            TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
                if (instructions.isEmpty()) {
                    Text("Chưa có hướng dẫn", color = TrainColors.Charcoal, fontSize = 13.sp)
                } else {
                    instructions.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", color = TrainColors.Charcoal, fontSize = 13.5.sp, lineHeight = 20.sp)
                        if (index < instructions.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Section("Mẹo tập đúng") {
            TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(16.dp)) {
                if (tips.isEmpty()) {
                    Text("Chưa có mẹo", color = TrainColors.Charcoal, fontSize = 13.sp)
                } else {
                    tips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tip, color = TrainColors.Charcoal, fontSize = 13.5.sp, lineHeight = 20.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToSessionSheet(
    exercise: ExerciseDto,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AddExerciseRequest) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            Box(Modifier.padding(top = 10.dp).size(width = 40.dp, height = 5.dp).clip(CircleShape).background(BottomSheetGrabber))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 21.dp, end = 21.dp, bottom = 28.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Thêm ${exercise.name}", color = TrainColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TrainRoundIconButton(icon = Icons.Default.Close, contentDescription = "Đóng", onClick = onDismiss, background = TrainColors.KeylimeWash, tint = TrainColors.Forest, size = 32.dp)
            }
            when (exercise.exerciseType.uppercase()) {
                "GYM" -> GymAddSheet(exercise, isSaving, onSubmit)
                "CARDIO" -> CardioAddSheet(exercise, isSaving, onSubmit)
                else -> SportAddSheet(exercise, isSaving, onSubmit)
            }
        }
    }
}

@Composable
private fun GymAddSheet(exercise: ExerciseDto, isSaving: Boolean, onSubmit: (AddExerciseRequest) -> Unit) {
    var sets by remember { mutableFloatStateOf((exercise.defaultSets ?: 3).toFloat()) }
    var reps by remember { mutableFloatStateOf((exercise.defaultReps ?: 10).toFloat()) }
    var weight by remember { mutableFloatStateOf(exercise.defaultWeightKg ?: 0f) }
    var rest by remember { mutableFloatStateOf((exercise.restTimeSeconds ?: 90).toFloat()) }
    var intensity by remember { mutableStateOf("MEDIUM") }
    val duration = ceil((sets * reps * 3 + (sets - 1).coerceAtLeast(0f) * rest) / 60f).roundToInt().coerceAtLeast(1)
    val kcal = ((gymIntensityMet(intensity) * 70f * duration) / 60f).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainNumberStepper("Sets", sets, { sets = it }, 1f, 1f, 20f, Modifier.weight(1f))
            TrainNumberStepper("Reps", reps, { reps = it }, 1f, 1f, 100f, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainNumberStepper("Tạ (kg)", weight, { weight = it }, 2.5f, 0f, 500f, Modifier.weight(1f), decimals = 1)
            TrainNumberStepper("Nghỉ (giây)", rest, { rest = it }, 15f, 0f, 600f, Modifier.weight(1f))
        }
        IntensitySelector(intensity) { intensity = it }
        CaloriePreview(duration, kcal, "${sets.roundToInt()}×${reps.roundToInt()} @ ${weight.roundToInt()}kg")
        TrainPrimaryButton(text = if (isSaving) "Đang thêm..." else "Thêm vào buổi tập", icon = Icons.Default.Check, enabled = !isSaving) {
            onSubmit(AddExerciseRequest(exercise.id, "GYM", sets.roundToInt(), reps.roundToInt(), weight, duration, 0, intensity, null, null, rest.roundToInt()))
        }
    }
}

@Composable
private fun SportAddSheet(exercise: ExerciseDto, isSaving: Boolean, onSubmit: (AddExerciseRequest) -> Unit) {
    var duration by remember { mutableFloatStateOf((exercise.defaultDurationMinutes ?: 30).toFloat()) }
    var intensity by remember { mutableStateOf(exercise.defaultIntensityLevel ?: "MEDIUM") }
    var distance by remember { mutableStateOf("") }
    val kcal = ((exercise.metValue.takeIf { it > 0f } ?: sportRate(intensity)) * duration).roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TrainNumberStepper("Thời lượng (phút)", duration, { duration = it }, 5f, 1f, 600f, Modifier.fillMaxWidth())
        IntensitySelector(intensity) { intensity = it }
        OutlinedTextField(
            value = distance,
            onValueChange = { distance = it },
            label = { Text("Khoảng cách (km) · tùy chọn") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrainColors.Forest, unfocusedBorderColor = TrainColors.Border, focusedContainerColor = TrainColors.Cream, unfocusedContainerColor = TrainColors.Cream)
        )
        CaloriePreview(duration.roundToInt(), kcal, intensityLabel(intensity))
        TrainPrimaryButton(text = if (isSaving) "Đang thêm..." else "Thêm vào buổi tập", icon = Icons.Default.Check, enabled = !isSaving) {
            onSubmit(AddExerciseRequest(exercise.id, "SPORT", null, null, null, duration.roundToInt(), 0, intensity, distance.toFloatOrNull(), null, null))
        }
    }
}

@Composable
private fun CardioAddSheet(exercise: ExerciseDto, isSaving: Boolean, onSubmit: (AddExerciseRequest) -> Unit) {
    var distance by remember { mutableFloatStateOf(5f) }
    var speed by remember { mutableFloatStateOf(10f) }
    val duration = cardioDuration(distance, speed).roundToInt()
    val kcal = cardioCalories(exercise, distance, speed)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TrainNumberStepper("Khoảng cách (km)", distance, { distance = it }, 0.5f, 0.1f, 1000f, Modifier.fillMaxWidth(), decimals = 1)
        TrainNumberStepper("Tốc độ trung bình (km/h)", speed, { speed = it }, 0.5f, 0.5f, 80f, Modifier.fillMaxWidth(), decimals = 1)
        TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                SimStat(duration.toString(), "Thời gian", "phút")
                SimStat(formatPace(speed), "Pace", "/km")
            }
        }
        CaloriePreview(duration, kcal, "${distance}km @ ${speed}km/h")
        TrainPrimaryButton(text = if (isSaving) "Đang thêm..." else "Thêm vào buổi tập", icon = Icons.Default.Check, enabled = !isSaving) {
            onSubmit(AddExerciseRequest(exercise.id, "CARDIO", null, null, null, duration, 0, null, distance, speed, null))
        }
    }
}

@Composable
private fun TrainNumberStepper(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    step: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    decimals: Int = 0
) {
    Column(modifier) {
        Text(label, color = TrainColors.Charcoal, fontSize = 13.sp, modifier = Modifier.padding(bottom = 7.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TrainColors.KeylimeWash).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepButton("-") { onChange((value - step).coerceIn(min, max)) }
            Text(
                if (decimals > 0) "%.${decimals}f".format(value) else value.roundToInt().toString(),
                color = TrainColors.Forest,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            StepButton("+") { onChange((value + step).coerceIn(min, max)) }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(TrainColors.Cream).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TrainColors.Forest, fontSize = 22.sp)
    }
}

@Composable
private fun IntensitySelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Cường độ", color = TrainColors.Charcoal, fontSize = 13.sp, modifier = Modifier.padding(bottom = 7.dp))
        TrainSegmented(
            options = listOf("LOW" to "Nhẹ", "MEDIUM" to "Vừa", "HIGH" to "Cao"),
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CaloriePreview(duration: Int, kcal: Int, extra: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TrainColors.Forest).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Ước tính tiêu hao", color = TrainColors.MintGlaze, fontSize = 11.5.sp)
            Text("$extra · ~$duration phút", color = TrainColors.MintGlaze.copy(alpha = 0.82f), fontSize = 12.sp)
        }
        Text("~$kcal kcal", color = TrainColors.Cream, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    }
}

private fun splitExerciseText(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    return text
        .replace(Regex("\\s+(?=\\d+[.)]\\s+)"), "\n")
        .lines()
        .map { it.trim().replace(Regex("^\\d+[.)]\\s*"), "").removePrefix("-").removePrefix("•").trim() }
        .filter { it.isNotBlank() }
}

private fun runningMet(speed: Float) = when {
    speed < 6f -> 3.5f
    speed < 8f -> 6f
    speed < 10f -> 8.3f
    speed < 12f -> 9.8f
    else -> 11f
}

private fun cardioDuration(distanceKm: Float, speedKmh: Float): Float = (distanceKm / speedKmh.coerceAtLeast(0.5f) * 60f).coerceAtLeast(1f)
private fun cardioCalories(exercise: ExerciseDto, distanceKm: Float, speedKmh: Float): Int {
    val met = if (exercise.metValue > 0f) exercise.metValue else runningMet(speedKmh)
    return (met * 70f * (distanceKm / speedKmh.coerceAtLeast(0.5f))).roundToInt()
}
private fun formatPace(speedKmh: Float): String {
    if (speedKmh <= 0f) return "--:--"
    val paceMin = 60f / speedKmh
    val mins = paceMin.toInt()
    val secs = ((paceMin - mins) * 60).roundToInt().coerceIn(0, 59)
    return "$mins:${secs.toString().padStart(2, '0')}"
}
private fun gymIntensityMet(intensity: String) = when (intensity) {
    "LOW" -> 3.5f
    "HIGH" -> 6.5f
    else -> 5f
}
private fun sportRate(intensity: String) = when (intensity) {
    "LOW" -> 4f
    "HIGH" -> 10f
    else -> 7f
}
private fun intensityLabel(intensity: String) = when (intensity.uppercase()) {
    "LOW" -> "Nhẹ"
    "HIGH" -> "Cao"
    else -> "Vừa"
}
