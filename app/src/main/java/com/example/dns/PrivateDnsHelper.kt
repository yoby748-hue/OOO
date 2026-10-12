package com.example.dns

import android.content.Context
import android.content.Intent
import android.provider.Settings

object PrivateDnsHelper {

    enum class PrivateDnsMode {
        OFF,
        OPPORTUNISTIC, // حالت خودکار
        STRICT,        // حالت هاست اختصاصی دستی
        UNKNOWN
    }

    fun getPrivateDnsMode(context: Context): PrivateDnsMode {
        return try {
            val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
            val specifier = Settings.Global.getString(context.contentResolver, "private_dns_specifier")

            when {
                mode.equals("off", ignoreCase = true) -> PrivateDnsMode.OFF
                mode.equals("hostname", ignoreCase = true) -> PrivateDnsMode.STRICT
                !specifier.isNullOrBlank() -> PrivateDnsMode.STRICT
                mode.equals("opportunistic", ignoreCase = true) -> PrivateDnsMode.OPPORTUNISTIC
                else -> PrivateDnsMode.UNKNOWN
            }
        } catch (_: Exception) {
            PrivateDnsMode.UNKNOWN
        }
    }

    fun openPrivateDnsSettings(context: Context) {
        try {
            val intent = Intent("android.settings.NETWORK_OPERATOR_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                val generalIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(generalIntent)
            }
        }
    }
}
