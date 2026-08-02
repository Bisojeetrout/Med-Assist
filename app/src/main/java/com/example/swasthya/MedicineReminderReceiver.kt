package com.example.swasthya

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class MedicineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Your Medicine"
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "NOTIFICATION"

        val isMeal = intent.getBooleanExtra("IS_MEAL", false)
        Log.d("MedicineReminder", "Reminder triggered for $medicineName ($reminderType) isMeal=$isMeal")

        if (reminderType == "ALARM") {
            // For alarm, we typically launch a full screen intent or play a loud sound.
            // For simplicity here, we'll fire a high-priority notification with an alarm sound
            // that acts like an alarm.
            fireNotification(context, medicineName, true, isMeal)
        } else {
            fireNotification(context, medicineName, false, isMeal)
        }
    }

    private fun fireNotification(context: Context, medicineName: String, isAlarm: Boolean, isMeal: Boolean) {
        val channelId = if (isAlarm) "swasthya_alarm_channel_v2" else "swasthya_notification_channel_v2"
        val channelName = if (isAlarm) "Medicine Alarms" else "Medicine Notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                if (isAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            )
            if (isAlarm) {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build()
                channel.setSound(alarmSound, audioAttributes)
                channel.enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val soundUri = if (isAlarm) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isAlarm) "Time to take $medicineName!" else "Medicine Reminder")
            .setContentText("It is time to take $medicineName.")
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setAutoCancel(true)

        if (isMeal) {
            val logMealIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_FOOD_LOG", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 
                1001, 
                logMealIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_add, "Log Meal", pendingIntent)
            
            // Also make the notification itself open the app to log meal
            builder.setContentIntent(pendingIntent)
        } else {
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 
                1002, 
                appIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }

        if (isAlarm) {
            // High priority alerts usually show as heads-up notifications
            builder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        }

        val notification = builder.build()
        if (isAlarm) {
            // This flag makes the notification sound loop continuously until dismissed, acting like a real alarm
            notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        }

        notificationManager.notify(medicineName.hashCode(), notification)
    }
}
