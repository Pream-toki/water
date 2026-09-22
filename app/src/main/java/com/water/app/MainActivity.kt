package com.water.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.water.app.di.ServiceLocator
import com.water.app.notify.ReminderScheduler
import com.water.app.ui.TodayScreen
import com.water.app.ui.TodayViewModel
import com.water.app.ui.theme.WaterTheme
import kotlinx.coroutines.launch

/**
 * Single screen. Zero-setup: the entire onboarding is one permission prompt;
 * defaults (8:00–22:00, 7/day) are already good.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: TodayViewModel by viewModels { TodayViewModel.Factory }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            lifecycleScope.launch {
                val repo = ServiceLocator.waterRepository(applicationContext)
                if (!repo.currentSettingsBlocking().onboarded) repo.markOnboarded()
                // Exact alarms don't need the permission; schedule either way,
                // notifications simply won't render until granted.
                ReminderScheduler.reschedule(applicationContext)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            WaterTheme(dynamicColor = state.settings?.dynamicColor ?: false) {
                TodayScreen(viewModel = viewModel)
            }
        }

        maybeRequestNotificationPermission()
    }

    private fun maybeRequestNotificationPermission() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!granted) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            lifecycleScope.launch {
                val repo = ServiceLocator.waterRepository(applicationContext)
                if (!repo.currentSettingsBlocking().onboarded) repo.markOnboarded()
                ReminderScheduler.reschedule(applicationContext)
            }
        }
    }
}
