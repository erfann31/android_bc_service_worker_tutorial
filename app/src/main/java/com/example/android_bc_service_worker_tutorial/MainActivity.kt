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
import android.os.FileObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File


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
            LogDisplayScreen(this)
//            BluetoothStatusDisplay(bluetoothStatus)
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

fun readLogsFromFile(context: Context): List<String> {
    val logFile = File(context.filesDir, "status_logs.txt")
    val logs = mutableListOf<String>()

    if (logFile.exists()) {
        logFile.useLines { lines ->
            lines.forEach { line ->
                val jsonObject = JSONObject(line)
                val timestamp = jsonObject.getString("timestamp")
                val bluetoothEnabled = jsonObject.getBoolean("bluetooth_enabled")
                val airplaneModeOn = jsonObject.getBoolean("airplane_mode_on")

                val logMessage =
                    "time: ${timestamp}\nBluetooth is ${if (bluetoothEnabled) "Enabled" else "Disabled"},\nAirplane mode is" + " ${
                        if
                                (airplaneModeOn) "On" else "Off"
                    }"
                logs.add(logMessage)
            }
        }
    }

    return logs.reversed()

}

class LogFileObserver(
    private val context: Context,
    private val callback: () -> Unit
) : FileObserver(context.filesDir.path + "/status_logs.txt", CREATE or MODIFY) {

    override fun onEvent(event: Int, path: String?) {
        if (event == CREATE || event == MODIFY) {
            callback()
        }
    }
}

@Composable
fun LogDisplayScreen(context: Context) {
    val logs = remember { mutableStateOf(readLogsFromFile(context)) }

    val observer = remember {
        LogFileObserver(context) {
            logs.value = readLogsFromFile(context)
        }
    }

    DisposableEffect(context) {
        observer.startWatching()
        onDispose {
            observer.stopWatching()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        items(logs.value) { log ->
            Text(text = log)
            Divider()
        }
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
