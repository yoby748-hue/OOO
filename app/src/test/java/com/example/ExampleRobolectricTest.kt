package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dns.DnsPreset
import com.example.dns.DnsPresets
import com.example.dns.DnsTester
import com.example.vpn.v2ray.V2RayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MESHKIDNS", appName)
  }

  @Test
  fun `test v2ray vless parsing`() {
    val vlessLink = "vless://b831381d-6324-4d53-ad4f-8cda48b30811@195.201.20.10:443?security=tls&type=ws&path=%2Fmeshki#Germany-V2Ray"
    val config = V2RayConfig.parse(vlessLink)
    assertNotNull(config)
    assertEquals("vless", config?.protocol)
    assertEquals("195.201.20.10", config?.server)
    assertEquals(443, config?.port)
    assertEquals("b831381d-6324-4d53-ad4f-8cda48b30811", config?.uuid)
    assertTrue(config?.isTls == true)
    assertTrue(config?.isWebSocket == true)
    assertEquals("/meshki", config?.path)
  }

  @Test
  fun `test v2ray trojan parsing`() {
    val trojanLink = "trojan://meshki_fast_pass_2026@95.217.100.50:443?security=tls&type=tcp#Finland-Trojan"
    val config = V2RayConfig.parse(trojanLink)
    assertNotNull(config)
    assertEquals("trojan", config?.protocol)
    assertEquals("95.217.100.50", config?.server)
    assertEquals(443, config?.port)
    assertEquals("meshki_fast_pass_2026", config?.uuid)
    assertTrue(config?.isTls == true)
  }

  @Test
  fun `test presets have vpn category`() {
    val vpnPresets = DnsPresets.presets.filter { it.category == "وی پی ان" }
    assertTrue(vpnPresets.isNotEmpty())
    assertTrue(vpnPresets.all { it.serverType == "vpn" && it.v2rayLink.isNotBlank() })
  }

  @Test
  fun `test railway user vless parsing and alpn`() {
    val railwayLink = "vless://872c5068-1cf4-c9ba-af48-b38b206b0e98@mehdish-production-a675.up.railway.app:443?encryption=none&security=tls&sni=mehdish-production-a675.up.railway.app&fp=chrome&alpn=http%2F1.1&insecure=0&allowInsecure=0&type=ws&host=mehdish-production-a675.up.railway.app&path=%2Fi%2FeG7LndRLw1ctRhLKrzCJs-K3efljzYorAlU2mA7OokRmpdVyvC8hxHS86LMhLMOhzLE5WISgGxT3NgN-KynPvmR39xi2Rg6vTx5wVyQMZQLn6lev2OxiUYlXtaqBswQsKN92em1mLRWZFVDekbU%2Fws%2F872c5068-1cf4-c9ba-af48-b38b206b0e98#Lunel-max%20%C2%B7%20VLESS"
    val config = V2RayConfig.parse(railwayLink)
    assertNotNull(config)
    assertEquals("vless", config?.protocol)
    assertEquals("mehdish-production-a675.up.railway.app", config?.server)
    assertEquals(443, config?.port)
    assertEquals("872c5068-1cf4-c9ba-af48-b38b206b0e98", config?.uuid)
    assertEquals("http/1.1", config?.alpn)
    assertEquals("mehdish-production-a675.up.railway.app", config?.sni)
    assertEquals("mehdish-production-a675.up.railway.app", config?.host)
    assertTrue(config?.isTls == true)
    assertTrue(config?.isWebSocket == true)
    assertTrue(config?.path?.contains("ws/872c5068") == true)
  }

  @Test
  fun `test user turkey vmess parsing`() {
    val vmessLink = "vmess://ew0KICAidiI6ICIyIiwNCiAgInBzIjogIlx1RDgzQ1x1RERGOVx1RDgzQ1x1RERGNyAzIFR1cmtleSAoU3RhdGljIElQKSIsDQogICJhZGQiOiAidHIxLXN0YXRpYy1pcC1tdWx0aS5tdWx0aXNlcnZlci1hZGVsc2hvb3AuY29tIiwNCiAgInBvcnQiOiAiODAiLA0KICAiaWQiOiAiMmY3ZDI2ZGEtZjI3Ny00ZDg4LWI1YzMtZDVkZDZhOTc0ZTc2IiwNCiAgImFpZCI6ICIwIiwNCiAgInNjeSI6ICJhdXRvIiwNCiAgIm5ldCI6ICJ3cyIsDQogICJ0eXBlIjogIm5vbmUiLA0KICAiaG9zdCI6ICIiLA0KICAicGF0aCI6ICIiLA0KICAidGxzIjogIiIsDQogICJzbmkiOiAiIiwNCiAgImFscG4iOiAiIiwNCiAgImZwIjogIiIsDQogICJpbnNlY3VyZSI6ICIwIiwNCiAgInZjbiI6ICIiLA0KICAicGNzIjogIiINCn0="
    val config = V2RayConfig.parse(vmessLink)
    assertNotNull(config)
    assertEquals("vmess", config?.protocol)
    assertEquals("tr1-static-ip-multi.multiserver-adelshoop.com", config?.server)
    assertEquals(80, config?.port)
    assertEquals("2f7d26da-f277-4d88-b5c3-d5dd6a974e76", config?.uuid)
    assertEquals("/", config?.path)
    assertTrue(config?.isWebSocket == true)
    assertFalse(config?.isTls == true)
    assertTrue(config?.remark?.contains("Turkey") == true)
  }

  @Test
  fun `test v2ray delay tester invalid link handling`() {
    kotlinx.coroutines.runBlocking {
      val res = DnsTester.testV2RayDelay("invalid://foo")
      assertFalse(res.isReachable)
      assertNull(res.latencyMs)
    }
  }

  @Test
  fun `test uptime formatting`() {
    val s = 3665L // 1 hour, 1 min, 5 sec
    val timeStr = String.format(java.util.Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    assertEquals("01:01:05", timeStr)
  }
}
