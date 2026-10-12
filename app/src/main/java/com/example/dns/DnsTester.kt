package com.example.dns

import com.example.vpn.v2ray.V2RayConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.system.measureTimeMillis

data class DnsPingResult(
    val latencyMs: Long?,
    val isReachable: Boolean,
    val detail: String
)

data class LiveResolutionResult(
    val success: Boolean,
    val resolvedIp: String?,
    val latencyMs: Long,
    val message: String
)

object DnsTester {

    suspend fun pingDns(dnsIp: String, timeoutMs: Int = 2000): DnsPingResult = withContext(Dispatchers.IO) {
        val cleanIp = dnsIp.trim()
        if (!DnsPacketUtils.isValidIpv4(cleanIp)) {
            return@withContext DnsPingResult(null, false, "آدرس نامعتبر است")
        }

        // Method 1: Real UDP DNS Query
        val udpResult = tryUdpDnsQuery(cleanIp, timeoutMs)
        if (udpResult != null) {
            return@withContext DnsPingResult(udpResult, true, "${udpResult}ms (UDP)")
        }

        // Method 2: Real TCP Port 53 Handshake (Standard DNS Port)
        val tcpResult = tryTcpPort53Ping(cleanIp, timeoutMs)
        if (tcpResult != null) {
            return@withContext DnsPingResult(tcpResult, true, "${tcpResult}ms (TCP)")
        }

        DnsPingResult(null, false, "تایم‌اوت / عدم پاسخگویی")
    }

    suspend fun testV2RayDelay(v2rayLink: String, timeoutMs: Int = 3500): DnsPingResult = withContext(Dispatchers.IO) {
        val config = V2RayConfig.parse(v2rayLink)
            ?: return@withContext DnsPingResult(null, false, "فرمت لینک کانفیگ نامعتبر است")

        var rawSocket: Socket? = null
        try {
            rawSocket = Socket()
            DnsVpnService.protectSocket(rawSocket)
            rawSocket.tcpNoDelay = true
            rawSocket.soTimeout = timeoutMs

            val elapsed = measureTimeMillis {
                val socketAddress = InetSocketAddress(config.server, config.port)
                rawSocket.connect(socketAddress, timeoutMs)

                var activeSocket: Socket = rawSocket

                if (config.isTls) {
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                    val sslFactory = sslContext.socketFactory
                    val sslSocket = sslFactory.createSocket(rawSocket, config.server, config.port, true) as SSLSocket
                    DnsVpnService.protectSocket(sslSocket)
                    sslSocket.soTimeout = timeoutMs
                    val params = sslSocket.sslParameters ?: SSLParameters()
                    if (config.sni.isNotBlank()) {
                        try {
                            params.serverNames = listOf(SNIHostName(config.sni))
                        } catch (_: Exception) {}
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val alpnList = if (config.alpn.isNotBlank()) {
                            config.alpn.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                        } else {
                            arrayOf("http/1.1")
                        }
                        params.applicationProtocols = alpnList
                    }
                    sslSocket.sslParameters = params
                    sslSocket.startHandshake()
                    activeSocket = sslSocket
                }

                if (config.isWebSocket) {
                    val inStream = activeSocket.getInputStream()
                    val outStream = activeSocket.getOutputStream()
                    val host = if (config.host.isNotBlank()) config.host else if (config.sni.isNotBlank()) config.sni else config.server
                    val randomKey = android.util.Base64.encodeToString(java.util.UUID.randomUUID().toString().toByteArray().take(16).toByteArray(), android.util.Base64.NO_WRAP)
                    val path = if (config.path.startsWith("/")) config.path else "/${config.path}"
                    val request = "GET $path HTTP/1.1\r\n" +
                                  "Host: $host\r\n" +
                                  "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36\r\n" +
                                  "Upgrade: websocket\r\n" +
                                  "Connection: Upgrade\r\n" +
                                  "Sec-WebSocket-Key: $randomKey\r\n" +
                                  "Sec-WebSocket-Version: 13\r\n\r\n"
                    outStream.write(request.toByteArray(Charsets.US_ASCII))
                    outStream.flush()
                    val buf = ByteArray(1024)
                    var read = 0
                    while (read < buf.size) {
                        val b = inStream.read()
                        if (b == -1) break
                        buf[read++] = b.toByte()
                        if (read >= 4 && buf[read-4] == 0x0D.toByte() && buf[read-3] == 0x0A.toByte() && buf[read-2] == 0x0D.toByte() && buf[read-1] == 0x0A.toByte()) {
                            break
                        }
                    }
                    val resp = String(buf, 0, read, Charsets.US_ASCII)
                    if (!resp.contains("101")) {
                        throw IllegalStateException("خطا در وب‌سوکت: ${resp.take(25).trim()}")
                    }
                }

                activeSocket.close()
            }
            val delay = elapsed.coerceAtLeast(1L)
            DnsPingResult(delay, true, "${delay}ms (${config.protocol.uppercase()})")
        } catch (e: Exception) {
            val msg = when {
                e is java.net.SocketTimeoutException -> "تایم‌اوت اتصال"
                e is java.net.ConnectException -> "خطا در اتصال به پورت سرور"
                e is java.net.UnknownHostException -> "آدرس دامنه یافت نشد"
                else -> "عدم پاسخگویی (${e.localizedMessage?.take(20) ?: "خطا"})"
            }
            DnsPingResult(null, false, msg)
        } finally {
            try { rawSocket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun verifyLiveResolution(dnsIp: String, domain: String = "google.com"): LiveResolutionResult = withContext(Dispatchers.IO) {
        val cleanIp = dnsIp.trim()
        if (!DnsPacketUtils.isValidIpv4(cleanIp)) {
            return@withContext LiveResolutionResult(false, null, 0, "آدرس آی‌پی نامعتبر است")
        }

        var socket: DatagramSocket? = null
        try {
            val query = createDnsQueryPacket(domain)
            val address = InetAddress.getByName(cleanIp)
            socket = DatagramSocket().apply {
                soTimeout = 2500
            }
            DnsVpnService.protectSocket(socket)

            val sendPacket = DatagramPacket(query, query.size, address, 53)
            val receiveBuffer = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            val elapsed = measureTimeMillis {
                socket.send(sendPacket)
                socket.receive(receivePacket)
            }

            if (receivePacket.length >= 12) {
                val resolvedIp = parseFirstIpv4Answer(receivePacket.data, receivePacket.length)
                if (resolvedIp != null) {
                    LiveResolutionResult(
                        success = true,
                        resolvedIp = resolvedIp,
                        latencyMs = elapsed.coerceAtLeast(1L),
                        message = "پاسخ معتبر دریافت شد ($resolvedIp)"
                    )
                } else {
                    LiveResolutionResult(
                        success = true,
                        resolvedIp = null,
                        latencyMs = elapsed.coerceAtLeast(1L),
                        message = "پاسخ دی‌ان‌اس دریافت شد (بدون رکورد A)"
                    )
                }
            } else {
                LiveResolutionResult(false, null, 0, "پاسخ ناقص یا نامعتبر بود")
            }
        } catch (e: Exception) {
            LiveResolutionResult(false, null, 0, "عدم پاسخگویی سرور (${e.localizedMessage ?: "تایم‌اوت"})")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun parseFirstIpv4Answer(data: ByteArray, length: Int): String? {
        try {
            if (length < 12) return null
            val anCount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
            if (anCount == 0) return null

            // Skip question section
            var offset = 12
            // Skip domain labels
            while (offset < length && data[offset].toInt() != 0) {
                val len = data[offset].toInt() and 0xFF
                if (len >= 192) { // compression pointer
                    offset += 2
                    break
                }
                offset += 1 + len
            }
            if (offset < length && data[offset].toInt() == 0) {
                offset += 1
            }
            // Skip QTYPE (2) + QCLASS (2)
            offset += 4

            // Parse Answer Section
            for (i in 0 until anCount) {
                if (offset >= length) break
                // Skip Name
                if ((data[offset].toInt() and 0xC0) == 0xC0) {
                    offset += 2
                } else {
                    while (offset < length && data[offset].toInt() != 0) {
                        offset += 1 + (data[offset].toInt() and 0xFF)
                    }
                    if (offset < length) offset += 1
                }
                if (offset + 10 > length) break
                val type = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                val rdLength = ((data[offset + 8].toInt() and 0xFF) shl 8) or (data[offset + 9].toInt() and 0xFF)
                offset += 10

                if (type == 1 && rdLength == 4 && offset + 4 <= length) {
                    val ip1 = data[offset].toInt() and 0xFF
                    val ip2 = data[offset + 1].toInt() and 0xFF
                    val ip3 = data[offset + 2].toInt() and 0xFF
                    val ip4 = data[offset + 3].toInt() and 0xFF
                    return "$ip1.$ip2.$ip3.$ip4"
                }
                offset += rdLength
            }
        } catch (_: Exception) {}
        return null
    }

    private fun tryUdpDnsQuery(dnsIp: String, timeoutMs: Int): Long? {
        var socket: DatagramSocket? = null
        return try {
            val query = createDnsQueryPacket("google.com")
            val address = InetAddress.getByName(dnsIp)
            socket = DatagramSocket().apply {
                soTimeout = timeoutMs
            }
            DnsVpnService.protectSocket(socket)

            val sendPacket = DatagramPacket(query, query.size, address, 53)
            val receiveBuffer = ByteArray(512)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            val elapsed = measureTimeMillis {
                socket.send(sendPacket)
                socket.receive(receivePacket)
            }

            if (receivePacket.length >= 12) {
                elapsed.coerceAtLeast(1L)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun tryTcpPort53Ping(dnsIp: String, timeoutMs: Int): Long? {
        var socket: Socket? = null
        return try {
            socket = Socket()
            DnsVpnService.protectSocket(socket)
            val socketAddress = InetSocketAddress(dnsIp, 53)
            val elapsed = measureTimeMillis {
                socket.connect(socketAddress, timeoutMs)
            }
            elapsed.coerceAtLeast(1L)
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    fun createDnsQueryPacket(domain: String): ByteArray {
        val query = mutableListOf<Byte>()
        // Transaction ID (2 bytes)
        query.add(0xAB.toByte())
        query.add(0xCD.toByte())
        // Flags: standard query, recursion desired (0x0100)
        query.add(0x01.toByte())
        query.add(0x00.toByte())
        // Questions count: 1 (0x0001)
        query.add(0x00.toByte())
        query.add(0x01.toByte())
        // Answer RRs: 0
        query.add(0x00.toByte())
        query.add(0x00.toByte())
        // Authority RRs: 0
        query.add(0x00.toByte())
        query.add(0x00.toByte())
        // Additional RRs: 0
        query.add(0x00.toByte())
        query.add(0x00.toByte())

        // Domain labels (e.g. 6google3com0)
        domain.split(".").forEach { label ->
            if (label.isNotEmpty()) {
                query.add(label.length.toByte())
                label.toByteArray(Charsets.US_ASCII).forEach { query.add(it) }
            }
        }
        query.add(0x00.toByte()) // End of domain

        // Type: A (IPv4 address) = 1
        query.add(0x00.toByte())
        query.add(0x01.toByte())
        // Class: IN (Internet) = 1
        query.add(0x00.toByte())
        query.add(0x01.toByte())

        return query.toByteArray()
    }
}
