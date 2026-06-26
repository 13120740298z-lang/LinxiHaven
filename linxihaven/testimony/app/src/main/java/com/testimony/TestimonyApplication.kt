package com.testimony

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.testimony.core.security.AppDisguiseManager
import com.testimony.core.storage.SecureDatabase
import com.testimony.util.Constants

class TestimonyApplication : Application() {

    lateinit var secureDatabase: SecureDatabase
        private set

    lateinit var appDisguiseManager: AppDisguiseManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize secure database
        secureDatabase = SecureDatabase(this)

        // Initialize app disguise manager
        appDisguiseManager = AppDisguiseManager(this)

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Recording channel
            val recordingChannel = NotificationChannel(
                Constants.CHANNEL_RECORDING,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Evidence recording notification"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(recordingChannel)

            // Time anchor channel
            val timeChannel = NotificationChannel(
                Constants.CHANNEL_TIME_ANCHOR,
                getString(R.string.notification_channel_time),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Time anchoring notification"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(timeChannel)
        }
    }

    companion object {
        lateinit var instance: TestimonyApplication
            private set
    }
}
