package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.example.dns.DnsPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

data class UserSession(
    val username: String,
    val sessionToken: String,
    val serverUrl: String,
    val expiresAt: Long,
    val remainingSeconds: Long,
    val remainingDays: Long,
    val remainingHours: Long,
    val remainingMinutes: Long,
    val note: String = ""
) {
    val formattedRemainingTime: String
        get() {
            return if (remainingDays > 0) {
                "$remainingDays روز و $remainingHours ساعت باقی‌مانده"
            } else if (remainingHours > 0) {
                "$remainingHours ساعت و $remainingMinutes دقیقه باقی‌مانده"
            } else if (remainingMinutes > 0) {
                "$remainingMinutes دقیقه باقی‌مانده"
            } else {
                "کمتر از یک دقیقه باقی‌مانده"
            }
        }
}

sealed class LoginResult {
    data class Success(val session: UserSession, val servers: List<DnsPreset>? = null) : LoginResult()
    data class Error(val message: String, val code: String? = null) : LoginResult()
}

sealed class VerifyResult {
    data class Valid(
        val remainingSeconds: Long,
        val remainingDays: Long,
        val remainingHours: Long,
        val remainingMinutes: Long
    ) : VerifyResult()
    data class Invalid(val message: String, val code: String) : VerifyResult()
    data class NetworkError(val message: String) : VerifyResult()
}

class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meshki_dns_auth", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_REMAINING_SECS = "remaining_secs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_NOTE = "note"
        private const val KEY_CACHED_SERVERS = "cached_servers_json"

        // Official authentication server URL - NEVER displayed in the UI
        const val DEFAULT_SERVER_URL = "https://gamenetrokh.ir/dns/admin.php"
    }

    fun getDeviceId(): String {
        var devId = prefs.getString(KEY_DEVICE_ID, null)
        if (devId.isNullOrBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }
            devId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                "and_" + androidId.take(12)
            } else {
                "dev_" + UUID.randomUUID().toString().replace("-", "").take(14)
            }
            prefs.edit().putString(KEY_DEVICE_ID, devId).apply()
        }
        return devId
    }

    fun getServerUrl(): String = DEFAULT_SERVER_URL

    fun isLoggedIn(): Boolean {
        val hasLoginFlag = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val username = prefs.getString(KEY_USERNAME, null)
        return hasLoginFlag && !username.isNullOrBlank()
    }

    fun getCurrentSession(): UserSession? {
        if (!isLoggedIn()) return null
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val token = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""
        val serverUrl = getServerUrl()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val remSecs = prefs.getLong(KEY_REMAINING_SECS, 0L)
        val note = prefs.getString(KEY_NOTE, "") ?: ""

        val remDays = remSecs / 86400L
        val remHours = (remSecs % 86400L) / 3600L
        val remMins = (remSecs % 3600L) / 60L

        return UserSession(
            username = username,
            sessionToken = token,
            serverUrl = serverUrl,
            expiresAt = expiresAt,
            remainingSeconds = remSecs,
            remainingDays = remDays,
            remainingHours = remHours,
            remainingMinutes = remMins,
            note = note
        )
    }

    /**
     * Authenticates user using both POST parameters and JSON fallback
     */
    suspend fun login(usernameInput: String, passwordInput: String): LoginResult = withContext(Dispatchers.IO) {
        val username = usernameInput.trim()
        val password = passwordInput.trim()

        if (username.isBlank()) {
            return@withContext LoginResult.Error("لطفاً نام کاربری را وارد کنید")
        }
        if (password.isBlank()) {
            return@withContext LoginResult.Error("لطفاً رمز عبور را وارد کنید")
        }

        val targetUrl = DEFAULT_SERVER_URL
        val deviceId = getDeviceId()

        try {
            // Attempt 1: Standard POST Form-Data (Standard for PHP $_POST['username'] and $_POST['password'])
            val formBody = FormBody.Builder()
                .add("action", "login")
                .add("username", username)
                .add("password", password)
                .add("device_id", deviceId)
                .build()

            val urlWithParams = if (targetUrl.contains("?")) {
                "$targetUrl&action=login&username=${URLEncoder.encode(username, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}&device_id=${URLEncoder.encode(deviceId, "UTF-8")}"
            } else {
                "$targetUrl?action=login&username=${URLEncoder.encode(username, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}&device_id=${URLEncoder.encode(deviceId, "UTF-8")}"
            }

            val request = Request.Builder()
                .url(urlWithParams)
                .post(formBody)
                .addHeader("User-Agent", "MESHKIDNS-Android/2.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            val parsedResult = parseAuthResponse(responseBody, username, password, targetUrl)
            if (parsedResult != null) {
                return@withContext parsedResult
            }

            // Attempt 2: JSON Payload if form post didn't return standard success/error
            val jsonPayload = JSONObject().apply {
                put("action", "login")
                put("username", username)
                put("password", password)
                put("device_id", deviceId)
            }
            val jsonBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val jsonRequest = Request.Builder()
                .url(targetUrl)
                .post(jsonBody)
                .addHeader("User-Agent", "MESHKIDNS-Android/2.0")
                .build()

            val jsonResponse = okHttpClient.newCall(jsonRequest).execute()
            val jsonResponseBody = jsonResponse.body?.string().orEmpty()

            val parsedJsonResult = parseAuthResponse(jsonResponseBody, username, password, targetUrl)
            if (parsedJsonResult != null) {
                return@withContext parsedJsonResult
            }

            return@withContext LoginResult.Error("پاسخ سرور نامعتبر بود. لطفاً از اتصال اینترنت خود اطمینان حاصل کنید.")

        } catch (e: Exception) {
            return@withContext LoginResult.Error("خطا در ارتباط با سرور: ${e.localizedMessage ?: "تایم‌اوت یا خطای شبکه"}")
        }
    }

    private fun parseAuthResponse(responseBody: String, username: String, password: String, targetUrl: String): LoginResult? {
        if (responseBody.isBlank()) return null

        val json = try {
            val startIdx = responseBody.indexOf("{")
            val endIdx = responseBody.lastIndexOf("}")
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                JSONObject(responseBody.substring(startIdx, endIdx + 1))
            } else {
                JSONObject(responseBody)
            }
        } catch (_: Exception) {
            return null
        }

        val success = json.optBoolean("success", false) ||
                json.optString("status", "").equals("success", ignoreCase = true) ||
                json.optInt("code", 0) == 200

        if (success) {
            val token = json.optString("session_token", json.optString("token", UUID.randomUUID().toString()))
            val rawExpiresAt = json.optLong("expires_at", 0L)
            val expiresAt = if (rawExpiresAt in 1..9999999999L) {
                rawExpiresAt * 1000L
            } else if (rawExpiresAt > 0) {
                rawExpiresAt
            } else {
                System.currentTimeMillis() + (30L * 86400000L)
            }

            val remSecs = json.optLong("remaining_seconds", ((expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(86400L))
            val remDays = json.optLong("remaining_days", remSecs / 86400L)
            val remHours = json.optLong("remaining_hours", (remSecs % 86400L) / 3600L)
            val remMins = json.optLong("remaining_minutes", (remSecs % 3600L) / 60L)
            val note = json.optString("note", "")

            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_SESSION_TOKEN, token)
                .putString(KEY_SERVER_URL, targetUrl)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .putLong(KEY_REMAINING_SECS, remSecs)
                .putString(KEY_NOTE, note)
                .apply()

            val session = UserSession(
                username = username,
                sessionToken = token,
                serverUrl = targetUrl,
                expiresAt = expiresAt,
                remainingSeconds = remSecs,
                remainingDays = remDays,
                remainingHours = remHours,
                remainingMinutes = remMins,
                note = note
            )

            // Parse server presets if returned by server
            val serverList = parseServersJson(json.optJSONArray("servers"))
            if (!serverList.isNullOrEmpty()) {
                saveCachedServers(serverList)
            }

            return LoginResult.Success(session, serverList)
        } else {
            val code = json.optString("code", "AUTH_FAILED")
            val message = json.optString("message", "")
                .ifBlank { json.optString("msg", "") }
                .ifBlank { json.optString("error", "نام کاربری یا رمز عبور اشتباه است یا حساب منقضی شده است.") }

            return LoginResult.Error(message, code)
        }
    }

    suspend fun verifySession(): VerifyResult = withContext(Dispatchers.IO) {
        val session = getCurrentSession() ?: return@withContext VerifyResult.Invalid("حساب کاربری فعال نیست", "NO_SESSION")
        val savedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        val deviceId = getDeviceId()
        val targetUrl = DEFAULT_SERVER_URL

        try {
            val formBody = FormBody.Builder()
                .add("action", "verify")
                .add("username", session.username)
                .add("password", savedPassword)
                .add("session_token", session.sessionToken)
                .add("device_id", deviceId)
                .build()

            val urlWithParams = if (targetUrl.contains("?")) {
                "$targetUrl&action=verify&username=${URLEncoder.encode(session.username, "UTF-8")}&password=${URLEncoder.encode(savedPassword, "UTF-8")}&session_token=${URLEncoder.encode(session.sessionToken, "UTF-8")}&device_id=${URLEncoder.encode(deviceId, "UTF-8")}"
            } else {
                "$targetUrl?action=verify&username=${URLEncoder.encode(session.username, "UTF-8")}&password=${URLEncoder.encode(savedPassword, "UTF-8")}&session_token=${URLEncoder.encode(session.sessionToken, "UTF-8")}&device_id=${URLEncoder.encode(deviceId, "UTF-8")}"
            }

            val request = Request.Builder()
                .url(urlWithParams)
                .post(formBody)
                .addHeader("User-Agent", "MESHKIDNS-Android/2.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (responseBody.isBlank()) {
                return@withContext VerifyResult.NetworkError("پاسخی از سرور دریافت نشد")
            }

            val json = try {
                val startIdx = responseBody.indexOf("{")
                val endIdx = responseBody.lastIndexOf("}")
                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    JSONObject(responseBody.substring(startIdx, endIdx + 1))
                } else {
                    JSONObject(responseBody)
                }
            } catch (_: Exception) {
                return@withContext VerifyResult.NetworkError("پاسخ سرور نامعتبر است")
            }

            val success = json.optBoolean("success", false) ||
                    json.optString("status", "").equals("success", ignoreCase = true) ||
                    json.optInt("code", 0) == 200

            if (success) {
                val rawExpiresAt = json.optLong("expires_at", session.expiresAt)
                val expiresAt = if (rawExpiresAt in 1..9999999999L) rawExpiresAt * 1000L else if (rawExpiresAt > 0) rawExpiresAt else session.expiresAt
                val remSecs = json.optLong("remaining_seconds", session.remainingSeconds)
                val remDays = json.optLong("remaining_days", remSecs / 86400L)
                val remHours = json.optLong("remaining_hours", (remSecs % 86400L) / 3600L)
                val remMins = json.optLong("remaining_minutes", (remSecs % 3600L) / 60L)

                prefs.edit()
                    .putLong(KEY_EXPIRES_AT, expiresAt)
                    .putLong(KEY_REMAINING_SECS, remSecs)
                    .apply()

                val serverList = parseServersJson(json.optJSONArray("servers"))
                if (!serverList.isNullOrEmpty()) {
                    saveCachedServers(serverList)
                }

                return@withContext VerifyResult.Valid(remSecs, remDays, remHours, remMins)
            } else {
                val code = json.optString("code", "SESSION_EXPIRED")
                val message = json.optString("message", "")
                    .ifBlank { json.optString("msg", "") }
                    .ifBlank { json.optString("error", "مدت اعتبار حساب شما به پایان رسیده است.") }

                logout()
                return@withContext VerifyResult.Invalid(message, code)
            }
        } catch (e: Exception) {
            return@withContext VerifyResult.NetworkError(e.localizedMessage ?: "خطای شبکه")
        }
    }

    suspend fun fetchServers(): List<DnsPreset>? = withContext(Dispatchers.IO) {
        val targetUrl = DEFAULT_SERVER_URL
        try {
            val fullUrl = if (targetUrl.contains("?")) {
                "$targetUrl&action=get_servers"
            } else {
                "$targetUrl?action=get_servers"
            }

            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .addHeader("User-Agent", "MESHKIDNS-Android/2.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (responseBody.isBlank()) return@withContext null

            val json = try {
                val startIdx = responseBody.indexOf("{")
                val endIdx = responseBody.lastIndexOf("}")
                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    JSONObject(responseBody.substring(startIdx, endIdx + 1))
                } else {
                    JSONObject(responseBody)
                }
            } catch (_: Exception) {
                return@withContext null
            }

            val serversArray = json.optJSONArray("servers")
            val list = parseServersJson(serversArray)
            if (!list.isNullOrEmpty()) {
                saveCachedServers(list)
                return@withContext list
            }
        } catch (_: Exception) {}
        return@withContext null
    }

    private fun parseServersJson(array: JSONArray?): List<DnsPreset>? {
        if (array == null || array.length() == 0) return null
        val list = mutableListOf<DnsPreset>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "srv_${i + 1}")
            val serverNumber = item.optString("server_number", "سرور ${i + 1}")
            val name = item.optString("name", "Server ${i + 1}")
            val primaryDns = item.optString("primary_dns", "1.1.1.1")
            val secondaryDns = item.optString("secondary_dns", "8.8.8.8")
            val rawCategory = item.optString("category", "تحریم‌شکن")
            val rawServerType = item.optString("server_type", "dns")
            val v2rayLink = item.optString("v2ray_link", "")

            val isVpn = rawServerType.equals("vpn", ignoreCase = true) ||
                    rawCategory.equals("vpn", ignoreCase = true) ||
                    rawCategory == "وی پی ان" ||
                    v2rayLink.isNotBlank()

            val category = when {
                isVpn -> "وی پی ان"
                rawCategory.equals("wireguard", ignoreCase = true) || rawCategory == "وایرگارد" -> "وایرگارد"
                rawCategory.equals("gaming", ignoreCase = true) || rawCategory == "گیمینگ" -> "گیمینگ"
                else -> "تحریم‌شکن"
            }

            val desc = when (category) {
                "وی پی ان" -> "سرور وی‌پی‌ان با تونل کامل دستگاه (V2Ray)"
                "وایرگارد" -> "سرور بهینه‌سازی شده شبکه وایرگارد"
                "گیمینگ" -> "سرور مخصوص بازی آنلاین و کاهش پینگ"
                else -> "سرور ضد تحریم و اتصال پرسرعت"
            }
            val tag = when (category) {
                "وی پی ان" -> "تونل کامل"
                "وایرگارد" -> "وایرگارد"
                "گیمینگ" -> "پینگ کم"
                else -> "آنتی‌تحریم"
            }

            list.add(
                DnsPreset(
                    id = id,
                    serverNumber = serverNumber,
                    name = name,
                    persianName = name,
                    primaryDns = primaryDns,
                    secondaryDns = secondaryDns,
                    description = desc,
                    category = category,
                    tag = tag,
                    serverType = if (isVpn) "vpn" else "dns",
                    v2rayLink = v2rayLink
                )
            )
        }
        return if (list.isNotEmpty()) list else null
    }

    fun saveCachedServers(servers: List<DnsPreset>) {
        val array = JSONArray()
        for (s in servers) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("server_number", s.serverNumber)
                put("name", s.name)
                put("primary_dns", s.primaryDns)
                put("secondary_dns", s.secondaryDns)
                put("category", s.category)
                put("server_type", s.serverType)
                put("v2ray_link", s.v2rayLink)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CACHED_SERVERS, array.toString()).apply()
    }

    fun getCachedServers(): List<DnsPreset>? {
        val jsonStr = prefs.getString(KEY_CACHED_SERVERS, null) ?: return null
        return try {
            val array = JSONArray(jsonStr)
            parseServersJson(array)
        } catch (_: Exception) {
            null
        }
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_REMAINING_SECS)
            .remove(KEY_NOTE)
            .apply()
    }
}
