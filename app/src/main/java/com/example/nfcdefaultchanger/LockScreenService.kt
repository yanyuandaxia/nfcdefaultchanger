package com.example.nfcdefaultchanger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LockScreenService : Service() {

    private val CHANNEL_ID = "lock_screen_service_channel"
    private val NOTIFICATION_ID = 1

    // 默认包名称为 "com.zte.nfc.sim"，可由启动服务时传入新值
    private var targetPackageName: String = "com.zte.nfc.sim"

    private lateinit var screenReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // 注册广播接收器，监听锁屏（屏幕关闭）事件
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    executeCommand()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }

    // 重写 onStartCommand 接收传递的包名称参数
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("extra_package_name")?.let {
            if (it.isNotEmpty()) {
                targetPackageName = it
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "锁屏监听服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("锁屏监听服务")
            .setContentText("正在监听锁屏事件")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }

    private fun executeCommand() {
        // 构造命令，使用输入的包名称替换默认值，
        // 格式为 "<package>/<package>"，例如 "com.example.app/com.example.app"
        val command = "settings put secure nfc_payment_default_component $targetPackageName/$targetPackageName"
        try {
            // 使用 su 权限执行命令（需要设备已 root）
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
