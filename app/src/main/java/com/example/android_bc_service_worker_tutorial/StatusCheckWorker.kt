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
        return Result.success()
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