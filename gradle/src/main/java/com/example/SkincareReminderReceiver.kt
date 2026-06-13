package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class SkincareReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule reminders on system reboot!
            rescheduleAllAlarms(context)
            return
        }

        val isAm = intent.getBooleanExtra("isAm", true)
        val title = if (isAm) "روتينكِ الصباحي بانتظارك! ☀️" else "روتينكِ المسائي حان الآن! 🌙"
        val message = if (isAm) {
            "حان الوقت لحماية بشرتكِ، موازنة الغدد الدهنية، وترطيبها لبدء يوم متوهج ونضر."
        } else {
            "دللي بشرتكِ بالنياسيناميد والترميم الفسيولوجي قبل النوم لإعادة بناء حاجزها."
        }

        showHeadsUpNotification(context, isAm, title, message)
    }

    private fun showHeadsUpNotification(context: Context, isAm: Boolean, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "skincare_reminders_channel"
        val notificationId = if (isAm) 111 else 222

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات بروتوكول زبيدة رمزي ✨",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات مخصصة لضمان إتمام روتينات الأدمة في مواعيدها بانتظام."
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app when notification clicked
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        fun rescheduleAllAlarms(context: Context) {
            val sharedPrefs = context.getSharedPreferences("skincare_reminder_prefs", Context.MODE_PRIVATE)
            
            val amEnabled = sharedPrefs.getBoolean("am_enabled", false)
            val amHour = sharedPrefs.getInt("am_hour", 8)
            val amMinute = sharedPrefs.getInt("am_minute", 0)

            val pmEnabled = sharedPrefs.getBoolean("pm_enabled", false)
            val pmHour = sharedPrefs.getInt("pm_hour", 20)
            val pmMinute = sharedPrefs.getInt("pm_minute", 0)

            if (amEnabled) {
                scheduleAlarm(context, true, amHour, amMinute)
            }
            if (pmEnabled) {
                scheduleAlarm(context, false, pmHour, pmMinute)
            }
        }

        fun scheduleAlarm(context: Context, isAm: Boolean, hour: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            
            val intent = Intent(context, SkincareReminderReceiver::class.java).apply {
                putExtra("isAm", isAm)
            }
            
            val requestCode = if (isAm) 1111 else 2222
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If set time is in the past, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                // Inexact repeating is highly recommended, starting from set calendar time daily
                alarmManager.setRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    android.app.AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun cancelAlarm(context: Context, isAm: Boolean) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            
            val intent = Intent(context, SkincareReminderReceiver::class.java)
            val requestCode = if (isAm) 1111 else 2222
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun triggerImmediateTestNotification(context: Context, isAm: Boolean) {
            val title = if (isAm) "فحص تجريبي: روتينكِ الصباحي ☀️" else "فحص تجريبي: روتينكِ المسائي 🌙"
            val message = if (isAm) {
                "هذا إشعار تجريبي فوري لروتين الأدمة الصباحي. تأكدي من حماية خلايا الكيراتين والوقاية من الشمس لتوهج يدوم طويلاً!"
            } else {
                "هذا إشعار تجريبي فوري للروتين المسائي الفسيولوجي. حان وقت النياسيناميد، البانثينول وتغذية السيراميدات العميقة ✨"
            }
            val receiver = SkincareReminderReceiver()
            receiver.showHeadsUpNotification(context, isAm, title, message)
        }
    }
}
