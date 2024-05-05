package com.example.android_bc_service_worker_tutorial

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BluetoothService : Service() {
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationChannelId: String
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun showBluetoothNotification(status: String) {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Bluetooth Status")
            .setContentText("Bluetooth is $status")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        notificationManager = NotificationManagerCompat.from(this)
        notificationChannelId = "bluetooth_channel"
        val channelName = "Bluetooth Status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(notificationChannelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return START_NOT_STICKY

        val isEnabled = bluetoothAdapter.isEnabled
        val status = if (isEnabled) "Connected" else "Disconnected"

        val broadcastIntent = Intent(ACTION_BLUETOOTH_STATUS)
        broadcastIntent.putExtra(EXTRA_STATUS, status)
        sendBroadcast(broadcastIntent)

        showBluetoothNotification(status)

        return START_STICKY
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val status = when (state) {
                BluetoothAdapter.STATE_ON -> "Connected"
                BluetoothAdapter.STATE_OFF -> "Disconnected"
                else -> "Unknown"
            }
            val broadcastIntent: Intent = Intent(ACTION_BLUETOOTH_STATUS)
            broadcastIntent.putExtra(EXTRA_STATUS, status)
            sendBroadcast(broadcastIntent)
            showBluetoothNotification(status)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
    }

    companion object {
        const val ACTION_BLUETOOTH_STATUS = "action.BLUETOOTH_STATUS"
        const val EXTRA_STATUS = "extra.STATUS"
    }
}
