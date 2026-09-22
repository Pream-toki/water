package com.water.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Short, quiet haptics for logging feedback. Requires the VIBRATE permission. */
object Haptics {

    private fun vibrator(context: Context): Vibrator? {
        val viaManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        return viaManager ?: context.getSystemService(Vibrator::class.java)
    }

    /** A single quiet click. Used by "I Drank 💧" and widget "+1". */
    fun confirm(context: Context) {
        val v = vibrator(context) ?: return
        v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
