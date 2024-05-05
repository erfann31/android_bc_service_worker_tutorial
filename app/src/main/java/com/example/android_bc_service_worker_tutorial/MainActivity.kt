package com.example.android_bc_service_worker_tutorial

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private var bluetoothStatus by mutableStateOf("Disconnected")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleInitialWork(this)
        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

        // If the permission is not granted, request it.
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            // Permission is already granted, create the notification channel
            createNotificationChannel()
        }


        setContent {
            BluetoothStatusDisplay(bluetoothStatus)
        }

        val bluetoothStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getStringExtra(BluetoothService.EXTRA_STATUS) ?: "Disconnected"
                bluetoothStatus = status
            }
        }

        val filter = IntentFilter(BluetoothService.ACTION_BLUETOOTH_STATUS)
        registerReceiver(bluetoothStatusReceiver, filter, RECEIVER_NOT_EXPORTED)

        lifecycleScope.launch {
            startService(Intent(this@MainActivity, BluetoothService::class.java))
        }
    }

    private val bluetoothStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getStringExtra(BluetoothService.EXTRA_STATUS) ?: "Disconnected"
            bluetoothStatus = status
        }

    }

    private fun createNotificationChannel() {
        // Create a notification channel
        val channelId = "bluetooth_channel"
        val channelName = "Bluetooth Status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStatusReceiver)
    }

}


@Composable
fun BluetoothStatusDisplay(status: String) {
    Column(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Bluetooth Status: $status")

    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BluetoothStatusDisplay("Disconnected")
}
