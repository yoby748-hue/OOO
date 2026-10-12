package com.example

import com.example.dns.DnsVpnService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsVpnTest {

    @Test
    fun testDnsServersConfiguration() {
        assertEquals("78.157.42.100", DnsVpnService.PRIMARY_DNS)
        assertEquals("78.157.42.101", DnsVpnService.SECONDARY_DNS)
    }

    @Test
    fun testConnectionStateFlow() {
        assertNotNull(DnsVpnService.isConnected)
        assertEquals(false, DnsVpnService.isConnected.value)
    }
}
