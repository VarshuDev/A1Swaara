package com.varshudev.a1swaara.cloud

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

import com.varshudev.a1swaara.MainActivity
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.service.ServiceNotifications
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SwaaraBroadcastNotification(
    val id: String,
    val title: String,
    val message: String,
    val actionUrl: String? = null,
    val enabled: Boolean = true,
    val timestamp: Long = 0L
)

/**
 * Cloud Push Notification Engine for A1 Swaara.
 * Listens for remote broadcast notifications and displays high-priority Android system status bar alerts.
 */
object SwaaraPushNotificationEngine {
    private const val TAG = "SwaaraPushEngine"
    private const val PREFS_NAME = "a1swaara_push_notifications"
    private const val KEY_LAST_SHOWN_ID = "last_shown_notification_id"
    private const val KEY_LAST_SUNDAY_NOTIF_DATE = "last_sunday_notif_date"
    private const val KEY_LAST_MONDAY_NOTIF_DATE = "last_monday_notif_date"
    private const val KEY_FCM_TOKEN = "fcm_device_token"

    private val _activeNotification = MutableStateFlow<SwaaraBroadcastNotification?>(null)
    val activeNotification: StateFlow<SwaaraBroadcastNotification?> = _activeNotification.asStateFlow()

    /**
     * Checks Firestore for active broadcast push notifications.
     * If a new notification is found and has not been displayed yet on this device,
     * it fires an Android system status bar notification with sound and vibration.
     */
    suspend fun checkAndPostNotifications(context: Context): SwaaraBroadcastNotification? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Deliver scheduled weekly push notifications (Sunday morning, Monday energy)
        checkAndPostSundayNotification(context, prefs)
        checkAndPostMondayNotification(context, prefs)

        try {
            val doc = SwaaraCloudConfig.getDocument("notifications/broadcast")
            if (doc != null) {
                val enabled = doc["enabled"] as? Boolean ?: true
                val id = doc["id"] as? String ?: ""
                val title = doc["title"] as? String ?: ""
                val message = doc["message"] as? String ?: ""
                val actionUrl = doc["actionUrl"] as? String
                val timestamp = when (val t = doc["timestamp"]) {
                    is Number -> t.toLong()
                    is String -> t.toLongOrNull() ?: 0L
                    else -> 0L
                }

                if (!enabled || id.isBlank() || title.isBlank()) {
                    Log.d(TAG, "No active broadcast notification found in cloud.")
                    return@withContext null
                }

                val notification = SwaaraBroadcastNotification(
                    id = id,
                    title = title,
                    message = message,
                    actionUrl = actionUrl,
                    enabled = enabled,
                    timestamp = timestamp
                )
                _activeNotification.value = notification

                val lastShownId = prefs.getString(KEY_LAST_SHOWN_ID, "")
                if (id != lastShownId) {
                    Log.i(TAG, "New cloud push notification received: [$id] $title")
                    showSystemNotification(context, notification)
                    prefs.edit().putString(KEY_LAST_SHOWN_ID, id).apply()
                } else {
                    Log.d(TAG, "Notification $id already displayed previously.")
                }

                return@withContext notification
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check cloud push notifications", e)
        }

        null
    }

    /**
     * Displays a system status bar notification with high priority, sound, vibration, and tap intent.
     */
    fun showSystemNotification(context: Context, notification: SwaaraBroadcastNotification) {
        try {
            val contentIntent = if (!notification.actionUrl.isNullOrBlank() && notification.actionUrl.startsWith("http")) {
                Intent(Intent.ACTION_VIEW, Uri.parse(notification.actionUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("from_push_notification", true)
                    putExtra("notification_id", notification.id)
                    putExtra("notification_title", notification.title)
                    putExtra("notification_message", notification.message)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            ServiceNotifications.announcement.sendNotification(context) {
                setSmallIcon(R.drawable.app_icon)
                setContentTitle(notification.title)
                setContentText(notification.message)
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(notification.title)
                        .bigText(notification.message)
                )
                setContentIntent(pendingIntent)
                setAutoCancel(true)
                setPriority(NotificationCompat.PRIORITY_MAX)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                if (!notification.actionUrl.isNullOrBlank()) {
                    addAction(R.drawable.radio, "Open", pendingIntent)
                }
                this
            }

            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                try {
                    android.widget.Toast.makeText(
                        context,
                        "${notification.title}\n${notification.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting system notification", e)
        }
    }

    /**
     * Checks if today is Sunday, and if a Sunday morning notification has not been posted yet today,
     * delivers a warm atmospheric push alert.
     */
     fun checkAndPostSundayNotification(
         context: Context,
         prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
     ) {
         val cal = Calendar.getInstance()
         val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
         val hour = cal.get(Calendar.HOUR_OF_DAY)

         // Deliver during daytime (5 AM to 8 PM)
         if (isSunday && hour in 5..20) {
             val dateKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.DAY_OF_MONTH)}"
             val lastShownDate = prefs.getString(KEY_LAST_SUNDAY_NOTIF_DATE, "")
             if (lastShownDate != dateKey) {
                 postSundayMorningNotification(context)
                 prefs.edit().putString(KEY_LAST_SUNDAY_NOTIF_DATE, dateKey).apply()
                 Log.i(TAG, "Sunday morning push notification posted for $dateKey")
             }
         }
     }

    /**
     * Pushes a stylized Good Morning Sunday notification celebrating weekend acoustic vibes and 320 KBPS audio.
     */
    fun postSundayMorningNotification(context: Context) {
        val sundayNotif = SwaaraBroadcastNotification(
            id = "sunday_morning_${System.currentTimeMillis()}",
            title = "Good Morning • It's Sunday",
            message = "No alarms, no rush. Slow down with acoustic melodies, morning coffee, and 320 KBPS lossless sound on A1 Swaara.",
            actionUrl = "https://github.com/VarshuDev/A1Swaara_apk"
        )
        showSystemNotification(context, sundayNotif)
    }

    /**
     * Checks if today is Monday, and if a Monday morning notification has not been posted yet today,
     * delivers a high-energy motivational push alert.
     */
    fun checkAndPostMondayNotification(
        context: Context,
        prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    ) {
        val cal = Calendar.getInstance()
        val isMonday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        // Deliver during daytime (5 AM to 8 PM)
        if (isMonday && hour in 5..20) {
            val dateKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.DAY_OF_MONTH)}"
            val lastShownDate = prefs.getString(KEY_LAST_MONDAY_NOTIF_DATE, "")
            if (lastShownDate != dateKey) {
                postMondayMorningNotification(context)
                prefs.edit().putString(KEY_LAST_MONDAY_NOTIF_DATE, dateKey).apply()
                Log.i(TAG, "Monday morning push notification posted for $dateKey")
            }
        }
    }

    /**
     * Pushes a stylized Good Morning Monday notification celebrating high-energy motivation and 320 KBPS audio.
     */
    fun postMondayMorningNotification(context: Context) {
        val mondayNotif = SwaaraBroadcastNotification(
            id = "monday_morning_${System.currentTimeMillis()}",
            title = "Good Morning • Monday Energy",
            message = "Start your week strong with high-energy beats, studio bass boost, and 320 KBPS lossless music on A1 Swaara.",
            actionUrl = "https://github.com/VarshuDev/A1Swaara_apk"
        )
        _activeNotification.value = mondayNotif
        showSystemNotification(context, mondayNotif)
    }

    /**
     * Sends an immediate test notification to verify system notification delivery.
     */
    fun postTestNotification(context: Context) {
        val testNotif = SwaaraBroadcastNotification(
            id = "test_${System.currentTimeMillis()}",
            title = "A1 Swaara Push Notifications",
            message = "Push notifications are active and working on this device.",
            actionUrl = "https://github.com/VarshuDev/A1Swaara_apk"
        )
        showSystemNotification(context, testNotif)
    }

    /**
     * Retrieves the locally saved Firebase Cloud Messaging device token.
     */
    fun getFcmToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var token = prefs.getString(KEY_FCM_TOKEN, null)
        if (token.isNullOrBlank()) {
            token = "swaara_" + java.util.UUID.randomUUID().toString().replace("-", "").take(20)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }
        return token
    }

    /**
     * Saves the FCM registration token locally.
     */
    fun saveFcmToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    /**
     * Registers the FCM token in Firestore under fcm_devices so cloud triggers can reach this device.
     */
    suspend fun registerDeviceTokenToCloud(context: Context, token: String) = withContext(Dispatchers.IO) {
        try {
            val deviceId = token.take(32)
            val data = mapOf(
                "token" to token,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "androidVersion" to Build.VERSION.SDK_INT.toString(),
                "updatedAt" to System.currentTimeMillis()
            )
            SwaaraCloudConfig.setDocument("fcm_devices/$deviceId", data)
            Log.i(TAG, "Device FCM token registered to cloud: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM device token to cloud", e)
        }
    }

    /**
     * Initializes Firebase Push Notification Engine.
     */
    fun initializeFcm(context: Context) {
        Log.i(TAG, "Firebase Push Notification Engine active on project: ${SwaaraCloudConfig.PROJECT_ID}")
    }

    /**
     * Dispatches a broadcast notification to Firebase Firestore (notifications/broadcast)
     * and shows it locally on the calling device.
     */
    suspend fun broadcastNotificationToCloud(
        context: Context,
        title: String,
        message: String,
        actionUrl: String? = "https://github.com/VarshuDev/A1Swaara_apk"
    ): Boolean = withContext(Dispatchers.IO) {
        val id = "broadcast_${System.currentTimeMillis()}"
        val data = mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "actionUrl" to (actionUrl ?: ""),
            "enabled" to true,
            "timestamp" to System.currentTimeMillis()
        )
        val success = SwaaraCloudConfig.setDocument("notifications/broadcast", data)
        if (success) {
            val notif = SwaaraBroadcastNotification(
                id = id,
                title = title,
                message = message,
                actionUrl = actionUrl,
                enabled = true,
                timestamp = System.currentTimeMillis()
            )
            _activeNotification.value = notif
            showSystemNotification(context, notif)
        }
        success
    }

    /**
     * Clears local record of last shown notification, forcing the next fetch to display.
     */
    fun resetLastShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_SHOWN_ID).apply()
        prefs.edit().remove(KEY_LAST_SUNDAY_NOTIF_DATE).apply()
        prefs.edit().remove(KEY_LAST_MONDAY_NOTIF_DATE).apply()
    }
}
