package com.water.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.water.app.domain.model.FrequencyPreset
import com.water.app.domain.model.UserSettings

/**
 * Minimal settings. Every change re-plans reminders immediately; nothing else
 * to configure. Includes the exact-alarm permission row for Android 14+
 * restricted state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: UserSettings?,
    onDismiss: () -> Unit,
    onFrequency: (FrequencyPreset) -> Unit,
    onActiveHours: (Int, Int) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
) {
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            // Frequency: Low 4 / Med 7 / High 10
            Column {
                Text(
                    "Reminders per day",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings?.frequencyPreset == preset,
                            onClick = { onFrequency(preset) },
                            label = { Text(preset.label()) },
                        )
                    }
                }
            }

            // Active hours
            Column {
                Text(
                    "Active hours",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { pickerTarget = PickerTarget.START }) {
                        Text(formatMinutes(settings?.activeStartMin ?: 480))
                    }
                    Text("to", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { pickerTarget = PickerTarget.END }) {
                        Text(formatMinutes(settings?.activeEndMin ?: 1320))
                    }
                }
            }

            // Exact alarm permission row (addendum #2)
            ExactAlarmsRow()

            // Dynamic color
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Material You colors", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Match your wallpaper",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings?.dynamicColor == true,
                    onCheckedChange = onDynamicColor,
                )
            }
        }
    }

    pickerTarget?.let { target ->
        val initialMinutes = when (target) {
            PickerTarget.START -> settings?.activeStartMin ?: 480
            PickerTarget.END -> settings?.activeEndMin ?: 1320
        }
        val state = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                Button(onClick = {
                    val picked = state.hour * 60 + state.minute
                    if (target == PickerTarget.START) {
                        onActiveHours(picked, settings?.activeEndMin ?: 1320)
                    } else {
                        onActiveHours(settings?.activeStartMin ?: 480, picked)
                    }
                    pickerTarget = null
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { pickerTarget = null }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun ExactAlarmsRow() {
    val context = LocalContext.current
    val allowed = rememberExactAlarmStatus()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Exact reminders", style = MaterialTheme.typography.titleMedium)
            val note = if (allowed) "Allowed — reminders arrive on time" else "Restricted — reminders may drift slightly"
            Text(
                note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!allowed) {
            TextButton(onClick = { openExactAlarmSettings(context) }) { Text("Allow") }
        }
    }
}

@Composable
private fun rememberExactAlarmStatus(): Boolean {
    val context = LocalContext.current
    return androidx.compose.runtime.remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() ?: false
        } else true
    }
}

private fun openExactAlarmSettings(context: Context) {
    // ACTION_REQUEST_SCHEDULE_EXACT_ALARM: Android 12+; deep-links to the
    // per-app exact-alarm screen (incl. Android 14+ restricted state).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

private enum class PickerTarget { START, END }

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val amPm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(h12, m, amPm)
}

private fun FrequencyPreset.label(): String = when (this) {
    FrequencyPreset.LOW -> "Low · 4"
    FrequencyPreset.MEDIUM -> "Med · 7"
    FrequencyPreset.HIGH -> "High · 10"
}
