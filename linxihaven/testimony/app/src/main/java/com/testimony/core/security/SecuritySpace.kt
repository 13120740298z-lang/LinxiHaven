package com.testimony.core.security

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.testimony.MainActivity
import com.testimony.R
import com.testimony.TestimonyApplication
import com.testimony.core.evidence.ScreenRecorderService
import com.testimony.core.timestamp.TimeAnchorService
import com.testimony.util.Constants
import kotlinx.coroutines.*

/**
 * Security Space Manager
 * Orchestrates secure recording environment with time anchoring and screen recording
 */
class SecuritySpace(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isActive = false
    private var screenRecorderService: ScreenRecorderService? = null
    private var timeAnchorService: TimeAnchorService? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Saved states for restoration
    private var originalRingerMode = AudioManager.RINGER_MODE_NORMAL
    private var originalVolume = 0

    /**
     * Initialize security space
     * - Start foreground recording
     * - Mute notifications
     * - Begin time anchoring
     */
    suspend fun enter(evidenceId: String): SecuritySpaceResult = withContext(Dispatchers.IO) {
        try {
            isActive = true

            // Save audio state
            originalRingerMode = audioManager.ringerMode
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

            // Mute notifications
            muteNotifications()

            // Start time anchoring service
            startTimeAnchoring()

            // Start screen recording
            val recordingResult = startScreenRecording(evidenceId)

            if (recordingResult.success) {
                // Show discreet notification
                showSecurityNotification()
            }

            SecuritySpaceResult(
                success = true,
                recordingPath = recordingResult.path,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            SecuritySpaceResult(
                success = false,
                error = e.message ?: "Failed to enter security space"
            )
        }
    }

    /**
     * Exit security space and restore normal state
     */
    suspend fun exit(): SecuritySpaceResult = withContext(Dispatchers.IO) {
        try {
            // Stop screen recording
            stopScreenRecording()

            // Stop time anchoring
            stopTimeAnchoring()

            // Restore audio state
            restoreAudio()

            // Hide notification
            hideSecurityNotification()

            isActive = false

            SecuritySpaceResult(
                success = true,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            SecuritySpaceResult(
                success = false,
                error = e.message ?: "Failed to exit security space"
            )
        }
    }

    /**
     * Mute all notifications during recording
     */
    private fun muteNotifications() {
        try {
            // Set DND mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }

            // Silent mode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (e: SecurityException) {
            // May not have permission, continue anyway
        }
    }

    /**
     * Restore audio to original state
     */
    private fun restoreAudio() {
        try {
            audioManager.ringerMode = originalRingerMode
        } catch (e: Exception) {
            // Restore failed, continue
        }
    }

    private fun startScreenRecording(evidenceId: String): RecordingResult {
        // In production, start ScreenRecorderService
        // For now, return mock result
        return RecordingResult(
            success = true,
            path = "${Constants.RECORDINGS_DIR}/$evidenceId.mp4"
        )
    }

    private fun stopScreenRecording() {
        // Stop recording service
    }

    private fun startTimeAnchoring() {
        // Start time anchor service
    }

    private fun stopTimeAnchoring() {
        // Stop time anchor service
    }

    private fun showSecurityNotification() {
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_RECORDING)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText(context.getString(R.string.notification_recording_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.startForeground(
                ScreenRecorderService.NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun hideSecurityNotification() {
        notificationManager.cancel(ScreenRecorderService.NOTIFICATION_ID)
    }

    /**
     * Haptic feedback for security events
     */
    fun provideSecureFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun isSecuritySpaceActive(): Boolean = isActive

    fun release() {
        scope.cancel()
    }

    data class SecuritySpaceResult(
        val success: Boolean,
        val recordingPath: String? = null,
        val timestamp: Long = 0,
        val error: String? = null
    )

    private data class RecordingResult(
        val success: Boolean,
        val path: String? = null,
        val error: String? = null
    )
}
