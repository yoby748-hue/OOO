package com.example.dns

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomDnsItem(
    val id: String,
    val name: String,
    val primaryDns: String = "",
    val secondaryDns: String = "",
    val serverType: String = "dns", // "dns" or "vpn"
    val v2rayLink: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class CustomDnsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meshki_custom_dns_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_SERVERS = "custom_dns_servers_list"
    }

    fun getAllCustomServers(): List<CustomDnsItem> {
        val raw = prefs.getString(KEY_CUSTOM_SERVERS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<CustomDnsItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CustomDnsItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        primaryDns = obj.optString("primaryDns", "1.1.1.1"),
                        secondaryDns = obj.optString("secondaryDns", ""),
                        serverType = obj.optString("serverType", if (obj.optString("v2rayLink", "").isNotBlank()) "vpn" else "dns"),
                        v2rayLink = obj.optString("v2rayLink", ""),
                        createdAt = obj.optLong("createdAt", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomServer(
        name: String,
        primaryDns: String = "1.1.1.1",
        secondaryDns: String = "",
        serverType: String = "dns",
        v2rayLink: String = ""
    ): CustomDnsItem {
        val currentList = getAllCustomServers().toMutableList()
        val newItem = CustomDnsItem(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name.trim().ifEmpty { if (serverType == "vpn") "کانفیگ اختصاصی" else "سرور من" },
            primaryDns = primaryDns.trim().ifBlank { "1.1.1.1" },
            secondaryDns = secondaryDns.trim(),
            serverType = serverType,
            v2rayLink = v2rayLink.trim(),
            createdAt = System.currentTimeMillis()
        )
        currentList.add(0, newItem)
        saveList(currentList)
        return newItem
    }

    fun deleteCustomServer(id: String) {
        val currentList = getAllCustomServers().filterNot { it.id == id }
        saveList(currentList)
    }

    private fun saveList(list: List<CustomDnsItem>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("primaryDns", item.primaryDns)
                put("secondaryDns", item.secondaryDns)
                put("serverType", item.serverType)
                put("v2rayLink", item.v2rayLink)
                put("createdAt", item.createdAt)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SERVERS, jsonArray.toString()).apply()
    }
}
