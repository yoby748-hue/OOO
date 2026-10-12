package com.example

import com.example.dns.DnsPacketUtils
import com.example.dns.DnsPresets
import com.example.dns.DnsTester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testIpv4Validation() {
    assertTrue(DnsPacketUtils.isValidIpv4("8.8.8.8"))
    assertTrue(DnsPacketUtils.isValidIpv4("1.1.1.1"))
    assertTrue(DnsPacketUtils.isValidIpv4("178.22.122.100"))
    assertTrue(DnsPacketUtils.isValidIpv4("78.157.42.101"))

    assertFalse(DnsPacketUtils.isValidIpv4("256.0.0.1"))
    assertFalse(DnsPacketUtils.isValidIpv4("8.8.8"))
    assertFalse(DnsPacketUtils.isValidIpv4("8.8.8.8.8"))
    assertFalse(DnsPacketUtils.isValidIpv4("abc.def.ghi.jkl"))
    assertFalse(DnsPacketUtils.isValidIpv4(""))
  }

  @Test
  fun testDnsQueryPacketCreation() {
    val packet = DnsTester.createDnsQueryPacket("google.com")
    assertTrue(packet.isNotEmpty())
    // Starts with transaction ID 0xAB, 0xCD
    assertEquals(0xAB.toByte(), packet[0])
    assertEquals(0xCD.toByte(), packet[1])
  }

  @Test
  fun testDnsPresetsCount() {
    assertTrue(DnsPresets.presets.size >= 6)
  }
}
