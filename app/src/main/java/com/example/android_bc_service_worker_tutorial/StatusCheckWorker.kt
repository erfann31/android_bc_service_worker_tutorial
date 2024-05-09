package com.example.android_bc_service_worker_tutorial

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class StatusCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private fun rescheduleWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val statusCheckRequest = OneTimeWorkRequestBuilder<StatusCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(statusCheckRequest)
    }

    override fun doWork(): Result {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false

        val isAirplaneModeOn = Settings.Global.getInt(
            applicationContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0

        Log.i("worker_bluetooth", "Bluetooth is ${if (isBluetoothEnabled) "Enabled" else "Disabled"}")
        Log.i("worker_airplane", "Airplane mode is ${if (isAirplaneModeOn) "On" else "Off"}")
        rescheduleWork()
        writeLogToFile(isBluetoothEnabled, isAirplaneModeOn)
        return Result.success()
    }

    private fun writeLogToFile(isBluetoothEnabled: Boolean, isAirplaneModeOn: Boolean) {
        val logFile = File(applicationContext.filesDir, "status_logs.txt")
        val jsonObject = JSONObject()
        jsonObject.put("timestamp", convertMillisToDate(System.currentTimeMillis()))
        jsonObject.put("bluetooth_enabled", isBluetoothEnabled)
        jsonObject.put("airplane_mode_on", isAirplaneModeOn)
        val jsonString = jsonObject.toString() + "\n"
        logFile.appendText(jsonString)
    }

    fun convertMillisToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}


fun scheduleInitialWork(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    val statusCheckRequest = OneTimeWorkRequestBuilder<StatusCheckWorker>()
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueue(statusCheckRequest)
}