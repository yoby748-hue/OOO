package com.example.dns

import java.nio.ByteBuffer

object DnsPacketUtils {

    fun isValidIpv4(ip: String): Boolean {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255 && (part == "0" || !part.startsWith("0"))
        }
    }

    data class ParsedDnsPacket(
        val isIpv6: Boolean,
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val payload: ByteArray
    )

    fun parseDnsPacket(packetData: ByteArray, length: Int): ParsedDnsPacket? {
        if (length < 28) return null
        val version = (packetData[0].toInt() ushr 4) and 0x0F
        return when (version) {
            4 -> parseIpv4Udp(packetData, length)
            6 -> parseIpv6Udp(packetData, length)
            else -> null
        }
    }

    // Alias for backward compatibility
    fun parseUdpPacket(packetData: ByteArray, length: Int): ParsedDnsPacket? =
        parseDnsPacket(packetData, length)

    private fun parseIpv4Udp(packetData: ByteArray, length: Int): ParsedDnsPacket? {
        val ihl = (packetData[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8) return null

        val protocol = packetData[9].toInt() and 0xFF
        if (protocol != 17) return null

        val sourceIp = packetData.copyOfRange(12, 16)
        val destIp = packetData.copyOfRange(16, 20)

        val srcPort = ((packetData[ihl].toInt() and 0xFF) shl 8) or (packetData[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packetData[ihl + 2].toInt() and 0xFF) shl 8) or (packetData[ihl + 3].toInt() and 0xFF)
        val udpLength = ((packetData[ihl + 4].toInt() and 0xFF) shl 8) or (packetData[ihl + 5].toInt() and 0xFF)

        if (udpLength < 8 || length < ihl + udpLength) return null

        val payloadLength = udpLength - 8
        val payload = packetData.copyOfRange(ihl + 8, ihl + 8 + payloadLength)

        return ParsedDnsPacket(
            isIpv6 = false,
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = srcPort,
            destPort = dstPort,
            payload = payload
        )
    }

    private fun parseIpv6Udp(packetData: ByteArray, length: Int): ParsedDnsPacket? {
        if (length < 48) return null
        val nextHeader = packetData[6].toInt() and 0xFF
        if (nextHeader != 17) return null

        val sourceIp = packetData.copyOfRange(8, 24)
        val destIp = packetData.copyOfRange(24, 40)

        val headerOffset = 40
        val srcPort = ((packetData[headerOffset].toInt() and 0xFF) shl 8) or (packetData[headerOffset + 1].toInt() and 0xFF)
        val dstPort = ((packetData[headerOffset + 2].toInt() and 0xFF) shl 8) or (packetData[headerOffset + 3].toInt() and 0xFF)
        val udpLength = ((packetData[headerOffset + 4].toInt() and 0xFF) shl 8) or (packetData[headerOffset + 5].toInt() and 0xFF)

        if (udpLength < 8 || length < headerOffset + udpLength) return null

        val payloadLength = udpLength - 8
        val payload = packetData.copyOfRange(headerOffset + 8, headerOffset + 8 + payloadLength)

        return ParsedDnsPacket(
            isIpv6 = true,
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = srcPort,
            destPort = dstPort,
            payload = payload
        )
    }

    fun buildUdpResponsePacketIpv4(
        serverIp: ByteArray,
        clientIp: ByteArray,
        serverPort: Int,
        clientPort: Int,
        payload: ByteArray
    ): ByteArray {
        val ipHeaderLength = 20
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + payload.size

        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // 1. IPv4 Header
        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0x4000.toShort())
        buffer.put(64.toByte())
        buffer.put(17.toByte())
        buffer.putShort(0.toShort())
        buffer.put(serverIp)
        buffer.put(clientIp)

        // 2. UDP Header
        buffer.position(ipHeaderLength)
        buffer.putShort(serverPort.toShort())
        buffer.putShort(clientPort.toShort())
        val udpTotalLen = udpHeaderLength + payload.size
        buffer.putShort(udpTotalLen.toShort())
        buffer.putShort(0.toShort())

        // 3. UDP Payload
        buffer.put(payload)

        // Checksums
        val ipChecksum = computeIpChecksum(packet, 0, ipHeaderLength)
        packet[10] = (ipChecksum ushr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        val udpChecksum = computeUdpChecksumIpv4(serverIp, clientIp, packet, ipHeaderLength, udpTotalLen)
        packet[ipHeaderLength + 6] = (udpChecksum ushr 8).toByte()
        packet[ipHeaderLength + 7] = (udpChecksum and 0xFF).toByte()

        return packet
    }

    fun buildUdpResponsePacketIpv6(
        serverIp: ByteArray,
        clientIp: ByteArray,
        serverPort: Int,
        clientPort: Int,
        payload: ByteArray
    ): ByteArray {
        val ipHeaderLength = 40
        val udpHeaderLength = 8
        val udpTotalLength = udpHeaderLength + payload.size
        val totalLength = ipHeaderLength + udpTotalLength

        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // 1. IPv6 Header
        buffer.putInt(0x60000000)
        buffer.putShort(udpTotalLength.toShort())
        buffer.put(17.toByte())
        buffer.put(64.toByte())
        buffer.put(serverIp)
        buffer.put(clientIp)

        // 2. UDP Header
        buffer.putShort(serverPort.toShort())
        buffer.putShort(clientPort.toShort())
        buffer.putShort(udpTotalLength.toShort())
        buffer.putShort(0.toShort())

        // 3. UDP Payload
        buffer.put(payload)

        // IPv6 Checksum
        val udpChecksum = computeUdpChecksumIpv6(serverIp, clientIp, packet, ipHeaderLength, udpTotalLength)
        packet[ipHeaderLength + 6] = (udpChecksum ushr 8).toByte()
        packet[ipHeaderLength + 7] = (udpChecksum and 0xFF).toByte()

        return packet
    }

    // Alias for backward compatibility
    fun buildUdpResponsePacket(
        serverIp: ByteArray,
        clientIp: ByteArray,
        serverPort: Int,
        clientPort: Int,
        payload: ByteArray
    ): ByteArray = buildUdpResponsePacketIpv4(serverIp, clientIp, serverPort, clientPort, payload)

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = if (i + 1 < offset + length) data[i + 1].toInt() and 0xFF else 0
            sum += (b1 shl 8) or b2
            i += 2
        }
        while ((sum ushr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun computeUdpChecksumIpv4(
        srcIp: ByteArray,
        dstIp: ByteArray,
        packet: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {
        var sum = 0
        sum += ((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)
        sum += ((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)
        sum += ((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)
        sum += ((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)
        sum += 17
        sum += udpLength

        var i = udpOffset
        while (i < udpOffset + udpLength) {
            val b1 = packet[i].toInt() and 0xFF
            val b2 = if (i + 1 < udpOffset + udpLength) packet[i + 1].toInt() and 0xFF else 0
            sum += (b1 shl 8) or b2
            i += 2
        }

        while ((sum ushr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = sum.inv() and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }

    private fun computeUdpChecksumIpv6(
        srcIp: ByteArray,
        dstIp: ByteArray,
        packet: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {
        var sum = 0
        for (k in 0 until 16 step 2) {
            sum += ((srcIp[k].toInt() and 0xFF) shl 8) or (srcIp[k + 1].toInt() and 0xFF)
        }
        for (k in 0 until 16 step 2) {
            sum += ((dstIp[k].toInt() and 0xFF) shl 8) or (dstIp[k + 1].toInt() and 0xFF)
        }
        sum += udpLength
        sum += 17

        var i = udpOffset
        while (i < udpOffset + udpLength) {
            val b1 = packet[i].toInt() and 0xFF
            val b2 = if (i + 1 < udpOffset + udpLength) packet[i + 1].toInt() and 0xFF else 0
            sum += (b1 shl 8) or b2
            i += 2
        }

        while ((sum ushr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = sum.inv() and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }
}
