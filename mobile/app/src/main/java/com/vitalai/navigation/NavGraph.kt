package com.vitalai.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vitalai.ui.components.VitalBottomNavBar
import com.vitalai.ui.screens.auth.SignInScreen
import com.vitalai.ui.screens.auth.SignUpScreen
import com.vitalai.ui.screens.auth.WelcomeScreen
import com.vitalai.ui.screens.auth.ForgotPasswordScreen
import com.vitalai.ui.screens.auth.ResetPasswordScreen
import com.vitalai.ui.screens.coach.ChatHistoryDrawer
import com.vitalai.ui.screens.coach.CoachScreen
import com.vitalai.ui.screens.coach.CoachViewModel
import com.vitalai.ui.screens.diary.CreateFoodScreen
import com.vitalai.ui.screens.diary.DiaryScreen
import com.vitalai.ui.screens.diary.FoodDetailScreen
import com.vitalai.ui.screens.diary.MealHistoryScreen
import com.vitalai.ui.screens.diary.SearchFoodScreen
import com.vitalai.ui.screens.discover.BlogComposerScreen
import com.vitalai.ui.screens.discover.BlogDetailScreen
import com.vitalai.ui.screens.discover.DiscoverScreen
import com.vitalai.ui.screens.discover.MyBlogsScreen
import com.vitalai.ui.screens.home.HomeScreen
import com.vitalai.ui.screens.metrics.MetricsHistoryScreen
import com.vitalai.ui.screens.metrics.MetricsScreen
import com.vitalai.ui.screens.goals.GoalsScreen
import com.vitalai.ui.screens.notifications.NotificationsScreen
import com.vitalai.ui.screens.onboarding.OnboardingScreen
import com.vitalai.ui.screens.profile.ProfileScreen
import com.vitalai.ui.screens.scan.ScanScreen
import com.vitalai.ui.screens.workout.ActivityScreen
import com.vitalai.ui.screens.workout.ExerciseDetailScreen
import com.vitalai.ui.screens.workout.ExerciseLibraryScreen
import com.vitalai.ui.screens.workout.WorkoutBuilderScreen
import com.vitalai.ui.screens.workout.WorkoutScreen
import com.vitalai.ui.screens.workout.WorkoutSessionScreen
import com.vitalai.ui.theme.AppMutedBackground
import kotlinx.coroutines.launch

/** Chừa khoảng status bar cố định cho một màn (inset intrinsic, không đổi theo route). */
@Composable
private fun WithStatusBarInset(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        content()
    }
}

/** Các route tab chính sở hữu thanh điều hướng dưới + nền muted dùng chung. */
private val mainTabRoutes = listOf(
    Screen.Home::class.qualifiedName,
    Screen.Diary::class.qualifiedName,
    Screen.Coach::class.qualifiedName,
    Screen.Profile::class.qualifiedName
)

/**
 * Vỏ ứng dụng cấp cao nhất: một Drawer + một Scaffold + một NavHost dùng chung.
 * Thanh điều hướng dưới và Drawer lịch sử Coach giờ là instance duy nhất ở đây,
 * thay vì để từng màn hình tab tự dựng lại.
 */
@Composable
fun VitalApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isCoach = currentRoute == Screen.Coach::class.qualifiedName
    val isMainTab = currentRoute in mainTabRoutes

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Rời khỏi Coach thì luôn đóng Drawer lại.
    LaunchedEffect(isCoach) { if (!isCoach) drawerState.close() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Chỉ cho kéo Drawer khi đang ở Coach; các màn khác khóa lại.
        gesturesEnabled = isCoach,
        drawerContent = {
            if (isCoach) {
                // Dùng chung đúng instance CoachViewModel với CoachScreen
                // (cùng NavBackStackEntry của Coach). Key theo backStackEntry để khi
                // quay lại Coach sẽ lấy entry mới, tránh giữ entry cũ đã rời stack.
                val coachEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.Coach) }
                val coachVm: CoachViewModel = hiltViewModel(coachEntry)
                val coachUi by coachVm.uiState.collectAsState()
                ChatHistoryDrawer(
                    sessions = coachUi.sessions,
                    currentSessionId = coachUi.currentSessionId,
                    onSelectSession = { sessionId ->
                        coachVm.selectSession(sessionId)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = coachVm::deleteSession,
                    onNewSession = {
                        coachVm.createNewSession()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = { if (isMainTab) VitalBottomNavBar(navController = navController) },
            // Nền muted sáng cố định cho mọi màn (đúng bằng nền Metrics/Workout...) ->
            // dải dưới chỗ thanh nav vừa rời đi không lộ màu nền theme (ở chế độ tối
            // colorScheme.background = GreenDark, gây "loé xanh"). Coach tự phủ trắng
            // vùng status bar bằng TopAppBar của nó.
            containerColor = AppMutedBackground,
            // Vỏ chung KHÔNG tiêu thụ status bar -> padding top của NavHost LUÔN = 0,
            // nên padding NavHost không đổi khi chuyển route -> màn đang fade-out không
            // bị "đẩy lên". Status bar do mỗi tab tự chừa (xem VitalNavGraph) / Coach
            // tự chừa bằng TopAppBar. (bottomBar vẫn tự dành 90dp cho các tab.)
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            VitalNavGraph(
                navController = navController,
                contentPadding = innerPadding,
                onOpenCoachDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}

@Composable
fun VitalNavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onOpenCoachDrawer: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome,
        modifier = Modifier.padding(contentPadding)
    ) {
        // ── Auth ──────────────────────────────────────────────
        composable<Screen.Welcome> {
            WelcomeScreen(navController)
        }
        composable<Screen.SignIn> {
            SignInScreen(navController)
        }
        composable<Screen.SignUp> {
            SignUpScreen(navController)
        }
        composable<Screen.ForgotPassword> {
            ForgotPasswordScreen(navController)
        }
        composable<Screen.ResetPassword> { backStackEntry ->
            val route: Screen.ResetPassword = backStackEntry.toRoute()
            ResetPasswordScreen(navController = navController, token = route.token)
        }
        composable<Screen.Onboarding> { backStackEntry ->
            val route: Screen.Onboarding = backStackEntry.toRoute()
            OnboardingScreen(route.step, navController)
        }

        // ── Main tabs ─────────────────────────────────────────
        // Home/Diary/Profile không có TopAppBar nên tự chừa status bar tại đây
        // (inset cố định theo từng màn -> không bị đẩy khi chuyển route).
        composable<Screen.Home> {
            WithStatusBarInset { HomeScreen(navController) }
        }
        composable<Screen.Diary> {
            WithStatusBarInset { DiaryScreen(navController) }
        }
        composable<Screen.Coach> {
            // Coach tự chừa status bar bằng TopAppBar của nó.
            CoachScreen(navController, onOpenDrawer = onOpenCoachDrawer)
        }
        composable<Screen.Profile> {
            WithStatusBarInset { ProfileScreen(navController) }
        }

        // ── Food / Diary sub-screens ──────────────────────────
        composable<Screen.SearchFood> { backStackEntry ->
            val route: Screen.SearchFood = backStackEntry.toRoute()
            SearchFoodScreen(navController, mealType = route.mealType, date = route.date)
        }
        composable<Screen.FoodDetail> { backStackEntry ->
            val route: Screen.FoodDetail = backStackEntry.toRoute()
            FoodDetailScreen(foodId = route.id, mealType = route.mealType, date = route.date, navController = navController)
        }
        composable<Screen.CreateFood> {
            CreateFoodScreen(navController)
        }
        composable<Screen.MealHistory> {
            MealHistoryScreen(navController)
        }

        // ── Workout / Metrics ─────────────────────────────────
        composable<Screen.Workout> {
            WorkoutScreen(navController)
        }
        composable<Screen.WorkoutSession> { backStackEntry ->
            val route: Screen.WorkoutSession = backStackEntry.toRoute()
            WorkoutSessionScreen(sessionId = route.id, date = route.date, navController = navController)
        }
        composable<Screen.Metrics> {
            MetricsScreen(navController)
        }
        composable<Screen.Goals> {
            GoalsScreen(navController)
        }

        // ── Discover / Blog ───────────────────────────────────
        composable<Screen.Discover> {
            DiscoverScreen(navController)
        }
        composable<Screen.BlogDetail> { backStackEntry ->
            val route: Screen.BlogDetail = backStackEntry.toRoute()
            BlogDetailScreen(blogId = route.id, navController = navController)
        }

        // ── Notifications ─────────────────────────────────────
        composable<Screen.Notifications> {
            NotificationsScreen(navController)
        }

        // ── AI Scan ───────────────────────────────────────────
        composable<Screen.Scan> {
            ScanScreen(navController)
        }

        // ── Training sub-screens ──────────────────────────────
        composable<Screen.Activity> {
            ActivityScreen(navController)
        }
        composable<Screen.ExerciseLibrary> {
            ExerciseLibraryScreen(navController)
        }
        composable<Screen.ExerciseDetail> { backStackEntry ->
            val route: Screen.ExerciseDetail = backStackEntry.toRoute()
            ExerciseDetailScreen(id = route.id, navController = navController)
        }
        composable<Screen.WorkoutBuilder> {
            WorkoutBuilderScreen(navController)
        }

        // ── Metrics sub-screens ───────────────────────────────
        composable<Screen.MetricsHistory> {
            MetricsHistoryScreen(navController)
        }

        // ── Blog sub-screens ──────────────────────────────────
        composable<Screen.BlogComposer> {
            BlogComposerScreen(navController)
        }
        composable<Screen.MyBlogs> {
            MyBlogsScreen(navController)
        }
    }
}
