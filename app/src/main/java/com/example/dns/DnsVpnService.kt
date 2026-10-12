package com.example.dns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_START = "com.example.dns.ACTION_START"
        const val ACTION_STOP = "com.example.dns.ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "anti_sanction_dns_channel"

        // سرورهای دی‌ان‌اس ضدتحریم (شکن مخصوص بازی‌ها)
        const val PRIMARY_DNS = "78.157.42.100"
        const val SECONDARY_DNS = "78.157.42.101"

        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, DnsVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DnsVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startDnsVpn()
            ACTION_STOP -> stopDnsVpn()
        }
        return START_NOT_STICKY
    }

    private fun startDnsVpn() {
        if (_isConnected.value) return

        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val builder = Builder()
                .setSession("Anti-Sanction Game DNS")
                // ۱. آی‌پی محلی با پیشوند ۳۲ بیت
                .addAddress("10.111.222.1", 32)
                // ۲. سرورهای دی‌ان‌اس ضدتحریم شکن
                .addDnsServer(PRIMARY_DNS)
                .addDnsServer(SECONDARY_DNS)
                // ۳. روت کردن فقط سرورهای DNS تا کل ترافیک بازی تونل نشود و پینگ تغییر نکند
                .addRoute(PRIMARY_DNS, 32)
                .addRoute(SECONDARY_DNS, 32)
                // ۴. تنظیم MTU روی ۱۵۰۰
                .setMtu(1500)

            // ۵. مستثنی کردن گوگل پلی و خود برنامه از تونل
            try {
                builder.addDisallowedApplication("com.android.vending")
            } catch (_: PackageManager.NameNotFoundException) {}

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {}

            builder.setBlocking(false)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                _isConnected.value = true
                startDnsForwarding(vpnInterface!!)
            } else {
                stopDnsVpn()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopDnsVpn()
        }
    }

    private fun startDnsForwarding(pfd: ParcelFileDescriptor) {
        workerJob?.cancel()
        workerJob = scope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val packet = ByteArray(32767)

            val forwardSocket = DatagramSocket()
            protect(forwardSocket)
            forwardSocket.soTimeout = 2500

            val primaryDnsAddr = InetAddress.getByName(PRIMARY_DNS)

            try {
                while (isActive) {
                    val length = try {
                        inputStream.read(packet)
                    } catch (_: Exception) {
                        -1
                    }
                    if (length <= 0) continue

                    val version = (packet[0].toInt().shr(4)) and 0x0F
                    if (version == 4 && length > 28) {
                        val protocol = packet[9].toInt() and 0xFF
                        if (protocol == 17) { // UDP
                            val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                            val destPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                                    (packet[ipHeaderLen + 3].toInt() and 0xFF)

                            if (destPort == 53) {
                                val udpPayloadOffset = ipHeaderLen + 8
                                val udpPayloadLen = length - udpPayloadOffset
                                if (udpPayloadLen > 0) {
                                    try {
                                        val sendPacket = DatagramPacket(
                                            packet,
                                            udpPayloadOffset,
                                            udpPayloadLen,
                                            primaryDnsAddr,
                                            53
                                        )
                                        forwardSocket.send(sendPacket)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { forwardSocket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun stopDnsVpn() {
        workerJob?.cancel()
        workerJob = null
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        _isConnected.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDnsVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "دی‌ان‌اس ضدتحریم بازی",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس فعال دی‌ان‌اس بازی بدون تغییر پینگ"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("دی‌ان‌اس ضدتحریم بازی فعال است")
            .setContentText("DNS: $PRIMARY_DNS | بدون تغییر پینگ")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
