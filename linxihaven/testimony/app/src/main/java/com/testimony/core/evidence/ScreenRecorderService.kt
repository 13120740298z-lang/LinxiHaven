package com.testimony.core.evidence

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import com.testimony.util.Constants
import com.testimony.util.generateUUID
import java.io.File
import java.io.IOException

/**
 * Screen Recording Service
 * Captures screen content with frame-accurate timestamps
 */
class ScreenRecorderService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.testimony.action.START_RECORDING"
        const val ACTION_STOP = "com.testimony.action.STOP_RECORDING"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_OUTPUT_PATH = "output_path"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputPath: String? = null

    private var projectionManager: MediaProjectionManager? = null
    private var resultCode: Int = Activity.RESULT_CANCELED
    private var resultData: Intent? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                    ?: getDefaultOutputPath()

                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    startRecording()
                } else {
                    Log.e(TAG, "Invalid projection data")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (isRecording) return

        try {
            // Create notification
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)

            // Initialize media projection
            mediaProjection = projectionManager?.getMediaProjection(resultCode, resultData!!)

            // Configure media recorder
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                // Video settings
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(5 * 1024 * 1024) // 5 Mbps
                setVideoFrameRate(30)
                setVideoSize(getScreenWidth(), getScreenHeight())

                // Audio settings (optional - can be disabled for privacy)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)

                // Output
                setOutputFile(outputPath)
            }

            // Create virtual display
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "TestimonyRecorder",
                getScreenWidth(),
                getScreenHeight(),
                getScreenDensity(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            // Start recording
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            isRecording = true

            Log.i(TAG, "Recording started: $outputPath")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            stopSelf()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            stopSelf()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        try {
            isRecording = false

            // Stop and release
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null

            virtualDisplay?.release()
            virtualDisplay = null

            mediaProjection?.stop()
            mediaProjection = null

            Log.i(TAG, "Recording stopped: $outputPath")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.CHANNEL_RECORDING)
            .setContentTitle("正在录制证据")
            .setContentText("应用已在后台安静录制")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun getScreenWidth(): Int {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels
    }

    private fun getScreenDensity(): Int {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.densityDpi
    }

    private fun getDefaultOutputPath(): String {
        val dir = File(filesDir, Constants.RECORDINGS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "recording_${generateUUID()}.mp4").absolutePath
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    /**
     * Get recording status
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * Get current recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        return mediaRecorder?.let {
            try {
                val currentPosition = it.currentPosition
                currentPosition.toLong()
            } catch (e: IllegalStateException) {
                0L
            }
        } ?: 0L
    }

    private object windowManager {
        fun getDefaultDisplay(): android.view.Display {
            return android.app.ActivityManager::class.java.let { am ->
                val field = am.getDeclaredField("mContext")
                field.isAccessible = true
                val systemService = android.app.ActivityManager::class.java.getDeclaredMethod(
                    "getSystemService",
                    String::class.java
                ).invoke(null, Context.WINDOW_SERVICE) as android.view.WindowManager
                @Suppress("DEPRECATION")
                systemService.defaultDisplay
            }
        }
    }

    companion object {
        private const val TAG = "ScreenRecorderService"

        /**
         * Start recording with activity result
         */
        fun startRecording(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            outputPath: String? = null
        ) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                outputPath?.let { putExtra(EXTRA_OUTPUT_PATH, it) }
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop recording
         */
        fun stopRecording(context: Context) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
