package com.vitalai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.vitalai.core.auth.SessionExpiryBus
import com.vitalai.core.notification.DeepLinkBus
import com.vitalai.core.notification.DeviceTokenRegistrar
import com.vitalai.core.notification.NotificationDeepLink
import com.vitalai.core.notification.VitalFirebaseMessagingService
import com.vitalai.core.sync.SyncScheduler
import com.vitalai.data.local.datastore.TokenManager
import com.vitalai.navigation.VitalApp
import com.vitalai.ui.theme.VitalAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import androidx.activity.enableEdgeToEdge

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var deepLinkBus: DeepLinkBus
    @Inject lateinit var deviceTokenRegistrar: DeviceTokenRegistrar
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var sessionExpiryBus: SessionExpiryBus

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* kết quả không bắt buộc xử lý */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleDeepLinkIntent(intent)
        ensureNotificationPermission()

        // Đọc token trước setContent — OS splash đang hiển thị nên user không thấy chờ.
        val startLoggedIn = runBlocking { tokenManager.accessToken.first() } != null

        // Đồng bộ FCM token nếu đã đăng nhập (no-op nếu chưa).
        lifecycleScope.launch { runCatching { deviceTokenRegistrar.ensureRegistered() } }

        // Vét hàng đợi ghi còn tồn (ghi offline trước đó) khi mở app.
        syncScheduler.requestSync()

        setContent {
            VitalAITheme {
                VitalApp(deepLinkBus = deepLinkBus, sessionExpiryBus = sessionExpiryBus, startLoggedIn = startLoggedIn)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    /** Đọc extras do notification gắn vào -> đẩy sang DeepLinkBus cho NavGraph xử lý. */
    private fun handleDeepLinkIntent(intent: Intent?) {
        val route = intent?.getStringExtra(VitalFirebaseMessagingService.EXTRA_ROUTE) ?: return
        val id = intent.getStringExtra(VitalFirebaseMessagingService.EXTRA_ID)
        deepLinkBus.emit(NotificationDeepLink(route = route, id = id))
        // Tránh xử lý lại extras khi Activity được tái tạo.
        intent.removeExtra(VitalFirebaseMessagingService.EXTRA_ROUTE)
        intent.removeExtra(VitalFirebaseMessagingService.EXTRA_ID)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
