package com.example.taskmanager.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.taskmanager.MainActivity
import com.example.taskmanager.data.UserPreferencesRepository
import com.example.taskmanager.data.dataStore
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.net.Uri

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Geofence - Odbieram sygnał GPS z tła", Toast.LENGTH_SHORT).show()

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Toast.makeText(context, "Geofence - Puste zdarzenie.", Toast.LENGTH_LONG).show()
            return
        }

        if (geofencingEvent.hasError()) {
            Toast.makeText(context, "Geofence -  Błąd nr: ${geofencingEvent.errorCode}", Toast.LENGTH_LONG).show()
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Toast.makeText(context, "Geofence: Przekroczono bar...", Toast.LENGTH_SHORT).show()

            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

             val repository = UserPreferencesRepository(context.dataStore)

            CoroutineScope(Dispatchers.IO).launch {
                val notificationsEnabled = repository.areNotificationsEnabled.first()

                if (!notificationsEnabled) {
                    return@launch
                }

                // generowanie powiadomienia
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "geofence_alarms"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Powiadomienia z lokalizacji", NotificationManager.IMPORTANCE_HIGH)
                    notificationManager.createNotificationChannel(channel)
                }

                for (geofence in triggeringGeofences) {
                    val taskId = geofence.requestId.substringBefore("_").toIntOrNull() ?: 0
                    val deepLinkIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("taskmanager://task/$taskId"),
                        context,
                        MainActivity::class.java
                    )
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        taskId,
                        deepLinkIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_map)
                        .setContentTitle("Osiągnąłeś cel")
                        .setContentText("Jesteś w pobliżu miejsca zadania.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent) // pozwala na przeniesienie do ekranu aplikacji
                        .setAutoCancel(true)
                        .build()

                    notificationManager.notify(taskId, notification)
                }
            }
        } else {
            Toast.makeText(context, "Geofence Zignorowano: Ruch typu $transition", Toast.LENGTH_SHORT).show()
        }
    }
}