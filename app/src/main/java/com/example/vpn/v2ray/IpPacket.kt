package com.example.vpn.v2ray

import java.net.InetAddress
import java.nio.ByteBuffer

class IpPacket(val rawData: ByteArray, val length: Int) {
    val version: Int = (rawData[0].toInt() and 0xF0) ushr 4
    val isIpv4: Boolean = version == 4
    val isIpv6: Boolean = version == 6

    val headerLength: Int = if (isIpv4) (rawData[0].toInt() and 0x0F) * 4 else 40
    val protocol: Int = if (isIpv4) rawData[9].toInt() and 0xFF else rawData[6].toInt() and 0xFF // 6 = TCP, 17 = UDP, 1 = ICMP

    val sourceIp: InetAddress
    val destIp: InetAddress

    val sourcePort: Int
    val destPort: Int

    val payloadOffset: Int
    val payloadLength: Int

    // TCP Flags
    val isTcp: Boolean = protocol == 6
    val isUdp: Boolean = protocol == 17
    val tcpSyn: Boolean
    val tcpAck: Boolean
    val tcpFin: Boolean
    val tcpRst: Boolean
    val tcpPsh: Boolean

    val tcpSeqNum: Long
    val tcpAckNum: Long

    init {
        if (isIpv4) {
            val src = ByteArray(4)
            System.arraycopy(rawData, 12, src, 0, 4)
            sourceIp = InetAddress.getByAddress(src)

            val dst = ByteArray(4)
            System.arraycopy(rawData, 16, dst, 0, 4)
            destIp = InetAddress.getByAddress(dst)
        } else {
            val src = ByteArray(16)
            System.arraycopy(rawData, 8, src, 0, 16)
            sourceIp = InetAddress.getByAddress(src)

            val dst = ByteArray(16)
            System.arraycopy(rawData, 24, dst, 0, 16)
            destIp = InetAddress.getByAddress(dst)
        }

        if (isTcp && length >= headerLength + 20) {
            val off = headerLength
            sourcePort = ((rawData[off].toInt() and 0xFF) shl 8) or (rawData[off + 1].toInt() and 0xFF)
            destPort = ((rawData[off + 2].toInt() and 0xFF) shl 8) or (rawData[off + 3].toInt() and 0xFF)

            val bb = ByteBuffer.wrap(rawData, off + 4, 8)
            tcpSeqNum = bb.int.toLong() and 0xFFFFFFFFL
            tcpAckNum = bb.int.toLong() and 0xFFFFFFFFL

            val tcpDataOffset = ((rawData[off + 12].toInt() and 0xF0) ushr 4) * 4
            val flags = rawData[off + 13].toInt() and 0xFF
            tcpFin = (flags and 0x01) != 0
            tcpSyn = (flags and 0x02) != 0
            tcpRst = (flags and 0x04) != 0
            tcpPsh = (flags and 0x08) != 0
            tcpAck = (flags and 0x10) != 0

            payloadOffset = off + tcpDataOffset
            payloadLength = (length - payloadOffset).coerceAtLeast(0)
        } else if (isUdp && length >= headerLength + 8) {
            val off = headerLength
            sourcePort = ((rawData[off].toInt() and 0xFF) shl 8) or (rawData[off + 1].toInt() and 0xFF)
            destPort = ((rawData[off + 2].toInt() and 0xFF) shl 8) or (rawData[off + 3].toInt() and 0xFF)

            tcpSyn = false
            tcpAck = false
            tcpFin = false
            tcpRst = false
            tcpPsh = false
            tcpSeqNum = 0L
            tcpAckNum = 0L

            payloadOffset = off + 8
            val udpLen = ((rawData[off + 4].toInt() and 0xFF) shl 8) or (rawData[off + 5].toInt() and 0xFF)
            payloadLength = (udpLen - 8).coerceIn(0, length - payloadOffset)
        } else {
            sourcePort = 0
            destPort = 0
            tcpSyn = false
            tcpAck = false
            tcpFin = false
            tcpRst = false
            tcpPsh = false
            tcpSeqNum = 0L
            tcpAckNum = 0L
            payloadOffset = headerLength
            payloadLength = (length - payloadOffset).coerceAtLeast(0)
        }
    }

    fun getPayload(): ByteArray {
        if (payloadLength <= 0) return ByteArray(0)
        val result = ByteArray(payloadLength)
        System.arraycopy(rawData, payloadOffset, result, 0, payloadLength)
        return result
    }

    companion object {
        fun buildTcpPacket(
            srcIp: InetAddress,
            dstIp: InetAddress,
            srcPort: Int,
            dstPort: Int,
            seqNum: Long,
            ackNum: Long,
            syn: Boolean = false,
            ack: Boolean = false,
            fin: Boolean = false,
            rst: Boolean = false,
            payload: ByteArray = ByteArray(0)
        ): ByteArray {
            val isIpv4 = srcIp.address.size == 4
            val ipHeaderLen = if (isIpv4) 20 else 40
            val tcpHeaderLen = 20
            val totalLen = ipHeaderLen + tcpHeaderLen + payload.size
            val packet = ByteArray(totalLen)

            if (isIpv4) {
                packet[0] = 0x45.toByte() // IPv4, Header length = 5 (20 bytes)
                packet[1] = 0x00
                packet[2] = ((totalLen ushr 8) and 0xFF).toByte()
                packet[3] = (totalLen and 0xFF).toByte()
                packet[4] = 0x12
                packet[5] = 0x34
                packet[6] = 0x40 // Don't fragment
                packet[7] = 0x00
                packet[8] = 64.toByte() // TTL
                packet[9] = 6.toByte() // TCP

                // IPs
                System.arraycopy(srcIp.address, 0, packet, 12, 4)
                System.arraycopy(dstIp.address, 0, packet, 16, 4)

                // IP Checksum
                val ipChecksum = calculateChecksum(packet, 0, 20)
                packet[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
                packet[11] = (ipChecksum and 0xFF).toByte()
            }

            val off = ipHeaderLen
            packet[off] = ((srcPort ushr 8) and 0xFF).toByte()
            packet[off + 1] = (srcPort and 0xFF).toByte()
            packet[off + 2] = ((dstPort ushr 8) and 0xFF).toByte()
            packet[off + 3] = (dstPort and 0xFF).toByte()

            // Seq Num
            packet[off + 4] = ((seqNum ushr 24) and 0xFF).toByte()
            packet[off + 5] = ((seqNum ushr 16) and 0xFF).toByte()
            packet[off + 6] = ((seqNum ushr 8) and 0xFF).toByte()
            packet[off + 7] = (seqNum and 0xFF).toByte()

            // Ack Num
            packet[off + 8] = ((ackNum ushr 24) and 0xFF).toByte()
            packet[off + 9] = ((ackNum ushr 16) and 0xFF).toByte()
            packet[off + 10] = ((ackNum ushr 8) and 0xFF).toByte()
            packet[off + 11] = (ackNum and 0xFF).toByte()

            packet[off + 12] = 0x50.toByte() // Data offset = 5 (20 bytes)

            var flags = 0
            if (fin) flags = flags or 0x01
            if (syn) flags = flags or 0x02
            if (rst) flags = flags or 0x04
            if (payload.isNotEmpty()) flags = flags or 0x08 // PSH flag: push data immediately to app
            if (ack) flags = flags or 0x10
            packet[off + 13] = flags.toByte()

            // Window Size (65535)
            packet[off + 14] = 0xFF.toByte()
            packet[off + 15] = 0xFF.toByte()

            if (payload.isNotEmpty()) {
                System.arraycopy(payload, 0, packet, off + tcpHeaderLen, payload.size)
            }

            // TCP Checksum with pseudo-header
            val tcpChecksum = calculateTcpChecksum(srcIp, dstIp, packet, off, tcpHeaderLen + payload.size)
            packet[off + 16] = ((tcpChecksum ushr 8) and 0xFF).toByte()
            packet[off + 17] = (tcpChecksum and 0xFF).toByte()

            return packet
        }

        fun buildIcmpPortUnreachable(originalPacket: IpPacket): ByteArray {
            val srcIp = originalPacket.destIp
            val dstIp = originalPacket.sourceIp
            val isIpv4 = srcIp.address.size == 4
            if (!isIpv4) return ByteArray(0)

            val ipHeaderLen = 20
            val icmpPayloadLen = 8 + 28
            val totalLen = ipHeaderLen + icmpPayloadLen
            val packet = ByteArray(totalLen)

            // IP Header
            packet[0] = 0x45.toByte()
            packet[1] = 0x00
            packet[2] = ((totalLen ushr 8) and 0xFF).toByte()
            packet[3] = (totalLen and 0xFF).toByte()
            packet[4] = 0x00
            packet[5] = 0x00
            packet[6] = 0x00
            packet[7] = 0x00
            packet[8] = 64.toByte()
            packet[9] = 1.toByte() // ICMP Protocol = 1

            System.arraycopy(srcIp.address, 0, packet, 12, 4)
            System.arraycopy(dstIp.address, 0, packet, 16, 4)

            val ipChecksum = calculateChecksum(packet, 0, 20)
            packet[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
            packet[11] = (ipChecksum and 0xFF).toByte()

            // ICMP Header (Type 3: Destination Unreachable, Code 3: Port Unreachable)
            val off = 20
            packet[off] = 3.toByte()
            packet[off + 1] = 3.toByte()
            packet[off + 2] = 0.toByte()
            packet[off + 3] = 0.toByte()
            packet[off + 4] = 0.toByte()
            packet[off + 5] = 0.toByte()
            packet[off + 6] = 0.toByte()
            packet[off + 7] = 0.toByte()

            val copyLen = originalPacket.length.coerceAtMost(28)
            System.arraycopy(originalPacket.rawData, 0, packet, off + 8, copyLen)

            val icmpChecksum = calculateChecksum(packet, off, icmpPayloadLen)
            packet[off + 2] = ((icmpChecksum ushr 8) and 0xFF).toByte()
            packet[off + 3] = (icmpChecksum and 0xFF).toByte()

            return packet
        }

        fun buildUdpPacket(
            srcIp: InetAddress,
            dstIp: InetAddress,
            srcPort: Int,
            dstPort: Int,
            payload: ByteArray
        ): ByteArray {
            val isIpv4 = srcIp.address.size == 4
            val ipHeaderLen = if (isIpv4) 20 else 40
            val udpHeaderLen = 8
            val totalLen = ipHeaderLen + udpHeaderLen + payload.size
            val packet = ByteArray(totalLen)

            if (isIpv4) {
                packet[0] = 0x45.toByte()
                packet[1] = 0x00
                packet[2] = ((totalLen ushr 8) and 0xFF).toByte()
                packet[3] = (totalLen and 0xFF).toByte()
                packet[4] = 0x56
                packet[5] = 0x78
                packet[6] = 0x00
                packet[7] = 0x00
                packet[8] = 64.toByte()
                packet[9] = 17.toByte() // UDP

                System.arraycopy(srcIp.address, 0, packet, 12, 4)
                System.arraycopy(dstIp.address, 0, packet, 16, 4)

                val ipChecksum = calculateChecksum(packet, 0, 20)
                packet[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
                packet[11] = (ipChecksum and 0xFF).toByte()
            }

            val off = ipHeaderLen
            val udpLen = udpHeaderLen + payload.size
            packet[off] = ((srcPort ushr 8) and 0xFF).toByte()
            packet[off + 1] = (srcPort and 0xFF).toByte()
            packet[off + 2] = ((dstPort ushr 8) and 0xFF).toByte()
            packet[off + 3] = (dstPort and 0xFF).toByte()
            packet[off + 4] = ((udpLen ushr 8) and 0xFF).toByte()
            packet[off + 5] = (udpLen and 0xFF).toByte()
            packet[off + 6] = 0x00
            packet[off + 7] = 0x00

            System.arraycopy(payload, 0, packet, off + udpHeaderLen, payload.size)

            return packet
        }

        private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
            var sum = 0
            var i = offset
            while (i < offset + length - 1) {
                val b1 = data[i].toInt() and 0xFF
                val b2 = data[i + 1].toInt() and 0xFF
                sum += (b1 shl 8) or b2
                i += 2
            }
            if (i < offset + length) {
                sum += (data[i].toInt() and 0xFF) shl 8
            }
            while ((sum shr 16) > 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            return (sum.inv()) and 0xFFFF
        }

        private fun calculateTcpChecksum(
            srcIp: InetAddress,
            dstIp: InetAddress,
            data: ByteArray,
            tcpOffset: Int,
            tcpLength: Int
        ): Int {
            var sum = 0
            val src = srcIp.address
            val dst = dstIp.address

            for (i in 0 until 4 step 2) {
                sum += ((src[i].toInt() and 0xFF) shl 8) or (src[i + 1].toInt() and 0xFF)
                sum += ((dst[i].toInt() and 0xFF) shl 8) or (dst[i + 1].toInt() and 0xFF)
            }

            sum += 6 // Protocol TCP
            sum += tcpLength

            var i = tcpOffset
            while (i < tcpOffset + tcpLength - 1) {
                if (i != tcpOffset + 16) { // skip checksum field itself
                    val b1 = data[i].toInt() and 0xFF
                    val b2 = data[i + 1].toInt() and 0xFF
                    sum += (b1 shl 8) or b2
                }
                i += 2
            }
            if (i < tcpOffset + tcpLength) {
                sum += (data[i].toInt() and 0xFF) shl 8
            }

            while ((sum shr 16) > 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            return (sum.inv()) and 0xFFFF
        }
    }
}
