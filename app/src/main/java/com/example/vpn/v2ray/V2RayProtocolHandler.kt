package com.example.vpn.v2ray

import android.net.VpnService
import android.os.Build
import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class V2RayProtocolHandler(
    private val vpnService: VpnService,
    private val config: V2RayConfig
) {

    class ProxyConnection(
        val socket: Socket,
        val inputStream: InputStream,
        val outputStream: OutputStream,
        val isWebSocket: Boolean,
        val protocol: String = "vless"
    ) {
        var isFirstResponse: Boolean = true
        var remainingWsFrameBytes: Long = 0L

        fun close() {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun resolveServerAddress(serverHost: String): InetAddress {
        // 1. If already an IP address
        try {
            val byName = InetAddress.getByName(serverHost)
            if (byName is java.net.Inet4Address) return byName
        } catch (_: Exception) {}

        // 2. Prefer IPv4 from system DNS
        try {
            val all = InetAddress.getAllByName(serverHost)
            val v4 = all.firstOrNull { it is java.net.Inet4Address }
            if (v4 != null) return v4
            if (all.isNotEmpty()) return all[0]
        } catch (_: Exception) {}

        // 3. Fallback: Resolve via DoH (bypasses Iranian mobile operator DNS blackhole)
        val dohEndpoints = listOf("https://1.1.1.1/dns-query", "https://8.8.8.8/dns-query")
        for (ep in dohEndpoints) {
            try {
                val url = java.net.URL("$ep?name=$serverHost&type=A")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                conn.setRequestProperty("Accept", "application/dns-json")
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(resp)
                    val answers = json.optJSONArray("Answer")
                    if (answers != null && answers.length() > 0) {
                        for (i in 0 until answers.length()) {
                            val ans = answers.getJSONObject(i)
                            if (ans.optInt("type") == 1) { // Type A (IPv4)
                                val ipStr = ans.optString("data")
                                if (ipStr.isNotBlank()) {
                                    return InetAddress.getByName(ipStr)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return InetAddress.getByName(serverHost)
    }

    fun openConnection(
        targetHost: String,
        targetPort: Int,
        isUdp: Boolean = false,
        timeoutMs: Int = 10000
    ): ProxyConnection {
        val serverIp = resolveServerAddress(config.server)
        val serverAddress = InetSocketAddress(serverIp, config.port)

        val rawSocket = Socket()
        vpnService.protect(rawSocket)
        rawSocket.soTimeout = timeoutMs
        rawSocket.tcpNoDelay = true
        rawSocket.keepAlive = true
        try { rawSocket.sendBufferSize = 65536 } catch (_: Exception) {}
        try { rawSocket.receiveBufferSize = 65536 } catch (_: Exception) {}

        rawSocket.connect(serverAddress, timeoutMs)

        var finalSocket: Socket = rawSocket

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
            vpnService.protect(sslSocket)

            try {
                sslSocket.enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")
            } catch (_: Exception) {}

            val sslParams = sslSocket.sslParameters ?: SSLParameters()
            val sniHost = if (config.sni.isNotBlank()) config.sni else config.server
            try {
                sslParams.serverNames = listOf(SNIHostName(sniHost))
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val alpnList = if (config.alpn.isNotBlank()) {
                    config.alpn.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                } else {
                    arrayOf("http/1.1")
                }
                sslParams.applicationProtocols = alpnList
            }
            sslSocket.sslParameters = sslParams
            sslSocket.startHandshake()
            finalSocket = sslSocket
        }

        val inStream = finalSocket.getInputStream()
        val outStream = finalSocket.getOutputStream()

        val isWs = config.isWebSocket
        if (isWs) {
            performWebSocketHandshake(inStream, outStream)
        } else if (config.isHttpObfuscated) {
            // TCP + HTTP Obfuscation header (headerType=http)
            val obfHost = if (config.host.isNotBlank()) config.host else if (config.sni.isNotBlank()) config.sni else config.server
            val obfPath = if (config.path.startsWith("/")) config.path else "/${config.path}"
            val httpReq = StringBuilder()
                .append("GET $obfPath HTTP/1.1\r\n")
                .append("Host: $obfHost\r\n")
                .append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36\r\n")
                .append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n")
                .append("Accept-Encoding: gzip, deflate\r\n")
                .append("Connection: keep-alive\r\n")
                .append("Pragma: no-cache\r\n")
                .append("\r\n")
            outStream.write(httpReq.toString().toByteArray(Charsets.US_ASCII))
            outStream.flush()
        }

        // Perform protocol-specific handshake
        when (config.protocol.lowercase()) {
            "vless" -> writeVlessHeader(outStream, targetHost, targetPort, isUdp, isWs)
            "trojan" -> writeTrojanHeader(outStream, targetHost, targetPort, isUdp, isWs)
            "vmess" -> writeVmessHeader(outStream, targetHost, targetPort, isUdp, isWs)
            "shadowsocks" -> writeShadowsocksHeader(outStream, targetHost, targetPort, isUdp, isWs)
            else -> writeVlessHeader(outStream, targetHost, targetPort, isUdp, isWs)
        }

        // Handshake finished - disable read timeout so idle connections (Telegram, browsing, background apps) don't get disconnected after 10s
        try {
            finalSocket.soTimeout = 0
            rawSocket.soTimeout = 0
        } catch (_: Exception) {}

        return ProxyConnection(finalSocket, inStream, outStream, isWs, config.protocol.lowercase())
    }

    private fun performWebSocketHandshake(inStream: InputStream, outStream: OutputStream) {
        val host = if (config.host.isNotBlank()) config.host else if (config.sni.isNotBlank()) config.sni else config.server
        val randomKey = Base64.encodeToString(UUID.randomUUID().toString().toByteArray().take(16).toByteArray(), Base64.NO_WRAP)
        val path = if (config.path.startsWith("/")) config.path else "/${config.path}"

        val request = StringBuilder()
            .append("GET $path HTTP/1.1\r\n")
            .append("Host: $host\r\n")
            .append("User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36\r\n")
            .append("Upgrade: websocket\r\n")
            .append("Connection: Upgrade\r\n")
            .append("Sec-WebSocket-Key: $randomKey\r\n")
            .append("Sec-WebSocket-Version: 13\r\n")
            .append("Accept-Language: en-US,en;q=0.9\r\n")
            .append("Pragma: no-cache\r\n")
            .append("Cache-Control: no-cache\r\n")
            .append("\r\n")

        outStream.write(request.toString().toByteArray(Charsets.US_ASCII))
        outStream.flush()

        // Read HTTP response status
        val buffer = ByteArray(4096)
        var bytesRead = 0

        while (bytesRead < buffer.size) {
            val b = inStream.read()
            if (b == -1) break
            buffer[bytesRead++] = b.toByte()
            if (bytesRead >= 4 &&
                buffer[bytesRead - 4] == 0x0D.toByte() &&
                buffer[bytesRead - 3] == 0x0A.toByte() &&
                buffer[bytesRead - 2] == 0x0D.toByte() &&
                buffer[bytesRead - 1] == 0x0A.toByte()
            ) {
                break
            }
        }

        val respHeader = String(buffer, 0, bytesRead, Charsets.US_ASCII)
        if (!respHeader.contains("101")) {
            throw IllegalStateException("خطا در ارتقای وب‌سوکت: ${respHeader.take(40)}")
        }
    }

    private fun writeVlessHeader(
        outStream: OutputStream,
        targetHost: String,
        targetPort: Int,
        isUdp: Boolean,
        isWs: Boolean
    ) {
        val uuidBytes = parseUuid(config.uuid)
        val targetAddrBytes = parseAddressVless(targetHost)

        val header = ByteBuffer.allocate(1 + 16 + 1 + 1 + 2 + targetAddrBytes.size)
        header.put(0x00.toByte()) // Version 0
        header.put(uuidBytes) // 16 bytes UUID
        header.put(0x00.toByte()) // Addons length 0
        header.put(if (isUdp) 0x02.toByte() else 0x01.toByte()) // Command (1 = TCP, 2 = UDP)
        header.putShort(targetPort.toShort()) // Port
        header.put(targetAddrBytes) // Address Type + Address

        val bytes = header.array()
        if (isWs) {
            writeWebSocketFrame(outStream, bytes, opcode = 0x02)
        } else {
            outStream.write(bytes)
            outStream.flush()
        }
    }

    private fun writeTrojanHeader(
        outStream: OutputStream,
        targetHost: String,
        targetPort: Int,
        isUdp: Boolean,
        isWs: Boolean
    ) {
        // SHA224 of password in hex (56 bytes)
        val sha224Hex = sha224Hex(config.uuid)
        val targetAddrBytes = parseAddressSocks5(targetHost)

        val header = ByteBuffer.allocate(56 + 2 + 1 + targetAddrBytes.size + 2 + 2)
        header.put(sha224Hex.toByteArray(Charsets.US_ASCII))
        header.put(0x0D.toByte())
        header.put(0x0A.toByte())
        header.put(if (isUdp) 0x03.toByte() else 0x01.toByte()) // Command (1 = TCP, 3 = UDP)
        header.put(targetAddrBytes)
        header.putShort(targetPort.toShort())
        header.put(0x0D.toByte())
        header.put(0x0A.toByte())

        val bytes = header.array()
        if (isWs) {
            writeWebSocketFrame(outStream, bytes, opcode = 0x02)
        } else {
            outStream.write(bytes)
            outStream.flush()
        }
    }

    private fun writeVmessHeader(
        outStream: OutputStream,
        targetHost: String,
        targetPort: Int,
        isUdp: Boolean,
        isWs: Boolean
    ) {
        val uuidBytes = parseUuid(config.uuid)
        val timestamp = System.currentTimeMillis() / 1000L
        val timeBuffer8 = ByteBuffer.allocate(8).putLong(timestamp).array()
        val timeBuffer32 = ByteBuffer.allocate(32).apply {
            putLong(timestamp)
            putLong(timestamp)
            putLong(timestamp)
            putLong(timestamp)
        }.array()

        // 1. 16-byte Auth Token = HMAC-MD5(UUID, timestamp)
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(uuidBytes, "HmacMD5"))
        val authToken = mac.doFinal(timeBuffer8)

        // 2. Request Header fields
        val reqBodyIV = ByteArray(16).apply { java.util.Random().nextBytes(this) }
        val reqBodyKey = ByteArray(16).apply { java.util.Random().nextBytes(this) }
        val respAuth = (java.util.Random().nextInt(256)).toByte()
        val opt = 0x01.toByte() // Standard chunking
        val pLen = 4
        val secType = 0x01.toByte() // 1 = None (within WS/TLS)
        val reserved = 0x00.toByte()
        val cmd = if (isUdp) 0x02.toByte() else 0x01.toByte()
        val addrBytes = parseAddressVless(targetHost)
        val paddingBytes = ByteArray(pLen).apply { java.util.Random().nextBytes(this) }

        val plainHeader = ByteBuffer.allocate(
            1 + 16 + 16 + 1 + 1 + 1 + 1 + 1 + 2 + addrBytes.size + pLen + 4
        )
        plainHeader.put(0x01.toByte()) // Version 1
        plainHeader.put(reqBodyIV) // 16 bytes
        plainHeader.put(reqBodyKey) // 16 bytes
        plainHeader.put(respAuth) // 1 byte
        plainHeader.put(opt) // 1 byte
        plainHeader.put((((pLen and 0x0F) shl 4) or (secType.toInt() and 0x0F)).toByte())
        plainHeader.put(reserved)
        plainHeader.put(cmd)
        plainHeader.putShort(targetPort.toShort())
        plainHeader.put(addrBytes)
        plainHeader.put(paddingBytes)

        // Calculate FNV-1a Checksum
        val plainBytesWithoutChecksum = plainHeader.array().copyOf(plainHeader.position())
        val checksum = fnv1a(plainBytesWithoutChecksum)
        plainHeader.putInt(checksum)

        val fullPlainBytes = plainHeader.array()

        // Encrypt command header with AES-128-CFB: Key = MD5(UUID), IV = MD5(timeBuffer32)
        val aesKey = md5(uuidBytes)
        val aesIV = md5(timeBuffer32)
        val encryptedHeader = encryptAesCfb(aesKey, aesIV, fullPlainBytes)

        // Final handshake payload = [Auth Token (16 bytes)] + [Encrypted Header]
        val finalPayload = ByteBuffer.allocate(authToken.size + encryptedHeader.size)
        finalPayload.put(authToken)
        finalPayload.put(encryptedHeader)

        val bytes = finalPayload.array()
        if (isWs) {
            writeWebSocketFrame(outStream, bytes, opcode = 0x02)
        } else {
            outStream.write(bytes)
            outStream.flush()
        }
    }

    private fun writeShadowsocksHeader(
        outStream: OutputStream,
        targetHost: String,
        targetPort: Int,
        isUdp: Boolean,
        isWs: Boolean
    ) {
        val targetAddrBytes = parseAddressSocks5(targetHost)
        val header = ByteBuffer.allocate(targetAddrBytes.size + 2)
        header.put(targetAddrBytes)
        header.putShort(targetPort.toShort())

        val bytes = header.array()
        if (isWs) {
            writeWebSocketFrame(outStream, bytes, opcode = 0x02)
        } else {
            outStream.write(bytes)
            outStream.flush()
        }
    }

    fun writeData(outStream: OutputStream, payload: ByteArray, isWs: Boolean, protocol: String = "vless") {
        if (payload.isEmpty()) return
        if (protocol == "vmess") {
            // Standard VMess data chunk: [2 bytes length] + [payload]
            val chunkBuffer = ByteBuffer.allocate(2 + payload.size)
            chunkBuffer.putShort(payload.size.toShort())
            chunkBuffer.put(payload)
            val chunkBytes = chunkBuffer.array()
            if (isWs) {
                writeWebSocketFrame(outStream, chunkBytes, opcode = 0x02)
            } else {
                outStream.write(chunkBytes)
                outStream.flush()
            }
        } else {
            if (isWs) {
                writeWebSocketFrame(outStream, payload, opcode = 0x02)
            } else {
                outStream.write(payload)
                outStream.flush()
            }
        }
    }

    fun readData(proxy: ProxyConnection, buffer: ByteArray): Int {
        val inStream = proxy.inputStream
        val isWs = proxy.isWebSocket

        if (!isWs) {
            val bytesRead = inStream.read(buffer)
            if (bytesRead <= 0) return bytesRead
            if (proxy.isFirstResponse) {
                proxy.isFirstResponse = false

                var startOffset = 0
                if (config.isHttpObfuscated) {
                    val crlfcrlf = byteArrayOf(0x0D, 0x0A, 0x0D, 0x0A)
                    val headerEnd = findSequence(buffer, bytesRead, crlfcrlf)
                    if (headerEnd != -1) {
                        startOffset = headerEnd + 4
                    }
                }

                if (proxy.protocol == "vless") {
                    val effectiveBytes = bytesRead - startOffset
                    if (effectiveBytes >= 2) {
                        val addonLen = buffer[startOffset + 1].toInt() and 0xFF
                        val headerLen = startOffset + 2 + addonLen
                        if (bytesRead > headerLen) {
                            val remaining = bytesRead - headerLen
                            System.arraycopy(buffer, headerLen, buffer, 0, remaining)
                            return remaining
                        } else {
                            return readData(proxy, buffer)
                        }
                    } else if (startOffset > 0 && bytesRead > startOffset) {
                        val remaining = bytesRead - startOffset
                        System.arraycopy(buffer, startOffset, buffer, 0, remaining)
                        return remaining
                    }
                } else if (proxy.protocol == "vmess") {
                    val headerLen = startOffset + 4
                    if (bytesRead > headerLen + 2) {
                        val remaining = bytesRead - headerLen - 2
                        System.arraycopy(buffer, headerLen + 2, buffer, 0, remaining)
                        return remaining
                    } else if (bytesRead > headerLen) {
                        val remaining = bytesRead - headerLen
                        System.arraycopy(buffer, headerLen, buffer, 0, remaining)
                        return remaining
                    }
                } else if (startOffset > 0 && bytesRead > startOffset) {
                    val remaining = bytesRead - startOffset
                    System.arraycopy(buffer, startOffset, buffer, 0, remaining)
                    return remaining
                }
            }
            return bytesRead
        }

        // If in middle of a previous WebSocket frame, continue reading its payload
        if (proxy.remainingWsFrameBytes > 0L) {
            val toRead = proxy.remainingWsFrameBytes.coerceAtMost(buffer.size.toLong()).toInt()
            val r = inStream.read(buffer, 0, toRead)
            if (r > 0) {
                proxy.remainingWsFrameBytes -= r
            }
            return r
        }

        // Read WebSocket frame
        var opcode = 0
        var payloadLen = 0L
        var isMasked = false
        var maskKey: ByteArray? = null

        while (true) {
            val b0 = inStream.read()
            if (b0 == -1) return -1
            val b1 = inStream.read()
            if (b1 == -1) return -1

            opcode = b0 and 0x0F
            if (opcode == 0x08) return -1 // Close frame

            payloadLen = (b1 and 0x7F).toLong()
            if (payloadLen == 126L) {
                val l0 = inStream.read()
                val l1 = inStream.read()
                if (l0 == -1 || l1 == -1) return -1
                payloadLen = ((l0 and 0xFF) shl 8 or (l1 and 0xFF)).toLong()
            } else if (payloadLen == 127L) {
                val bytes8 = ByteArray(8)
                var read = 0
                while (read < 8) {
                    val r = inStream.read(bytes8, read, 8 - read)
                    if (r == -1) return -1
                    read += r
                }
                payloadLen = ByteBuffer.wrap(bytes8).long
            }

            isMasked = (b1 and 0x80) != 0
            maskKey = if (isMasked) {
                val m = ByteArray(4)
                inStream.read(m)
                m
            } else null

            // If Ping (0x09), reply with Pong (0x0A) and continue
            if (opcode == 0x09) {
                val pingData = ByteArray(payloadLen.toInt())
                var rPing = 0
                while (rPing < pingData.size) {
                    val r = inStream.read(pingData, rPing, pingData.size - rPing)
                    if (r == -1) break
                    rPing += r
                }
                try {
                    writeWebSocketFrame(proxy.outputStream, pingData, opcode = 0x0A)
                } catch (_: Exception) {}
                continue
            }
            break
        }

        val toRead = payloadLen.coerceAtMost(buffer.size.toLong()).toInt()
        var totalRead = 0
        while (totalRead < toRead) {
            val r = inStream.read(buffer, totalRead, toRead - totalRead)
            if (r == -1) break
            totalRead += r
        }

        proxy.remainingWsFrameBytes = (payloadLen - totalRead).coerceAtLeast(0L)

        if (isMasked && maskKey != null) {
            for (i in 0 until totalRead) {
                buffer[i] = (buffer[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
        }

        // Strip handshake response header on first response
        if (proxy.isFirstResponse) {
            proxy.isFirstResponse = false
            if (proxy.protocol == "vless") {
                if (totalRead >= 2) {
                    val addonLen = buffer[1].toInt() and 0xFF
                    val headerLen = 2 + addonLen
                    if (totalRead > headerLen) {
                        val remaining = totalRead - headerLen
                        System.arraycopy(buffer, headerLen, buffer, 0, remaining)
                        return remaining
                    } else {
                        return readData(proxy, buffer)
                    }
                }
            } else if (proxy.protocol == "vmess") {
                val headerLen = 4
                if (totalRead > headerLen + 2) {
                    val remaining = totalRead - headerLen - 2
                    System.arraycopy(buffer, headerLen + 2, buffer, 0, remaining)
                    return remaining
                } else if (totalRead > headerLen) {
                    val remaining = totalRead - headerLen
                    System.arraycopy(buffer, headerLen, buffer, 0, remaining)
                    return remaining
                }
            }
        }

        return totalRead
    }

    private fun writeWebSocketFrame(outStream: OutputStream, data: ByteArray, opcode: Int = 0x02) {
        val len = data.size
        val mask = ByteArray(4).apply {
            val r = java.util.Random()
            r.nextBytes(this)
        }

        val frameHeader = mutableListOf<Byte>()
        frameHeader.add((0x80 or (opcode and 0x0F)).toByte()) // FIN + Opcode (0x82 for Binary)

        if (len < 126) {
            frameHeader.add((0x80 or len).toByte()) // Masked = 1
        } else if (len <= 65535) {
            frameHeader.add((0x80 or 126).toByte())
            frameHeader.add(((len ushr 8) and 0xFF).toByte())
            frameHeader.add((len and 0xFF).toByte())
        } else {
            frameHeader.add((0x80 or 127).toByte())
            for (i in 7 downTo 0) {
                frameHeader.add(((len.toLong() ushr (i * 8)) and 0xFF).toByte())
            }
        }

        frameHeader.addAll(mask.toList())

        val maskedData = ByteArray(len)
        for (i in 0 until len) {
            maskedData[i] = (data[i].toInt() xor mask[i % 4].toInt()).toByte()
        }

        synchronized(outStream) {
            outStream.write(frameHeader.toByteArray())
            outStream.write(maskedData)
            outStream.flush()
        }
    }

    private fun parseUuid(uuidStr: String): ByteArray {
        return try {
            val clean = uuidStr.replace("-", "")
            val bytes = ByteArray(16)
            for (i in 0 until 16) {
                bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            bytes
        } catch (_: Exception) {
            UUID.randomUUID().toString().replace("-", "").take(16).toByteArray()
        }
    }

    private fun parseAddressVless(targetHost: String): ByteArray {
        val host = targetHost.trim()
        val ip = try { InetAddress.getByName(host) } catch (_: Exception) { null }
        return if (ip != null) {
            val addr = ip.address
            if (addr.size == 4) {
                val b = ByteArray(1 + 4)
                b[0] = 0x01.toByte() // IPv4
                System.arraycopy(addr, 0, b, 1, 4)
                b
            } else {
                val b = ByteArray(1 + 16)
                b[0] = 0x03.toByte() // IPv6
                System.arraycopy(addr, 0, b, 1, 16)
                b
            }
        } else {
            val domainBytes = host.toByteArray(Charsets.US_ASCII)
            val b = ByteArray(1 + 1 + domainBytes.size)
            b[0] = 0x02.toByte() // Domain in VLESS / VMess
            b[1] = domainBytes.size.toByte()
            System.arraycopy(domainBytes, 0, b, 2, domainBytes.size)
            b
        }
    }

    private fun parseAddressSocks5(targetHost: String): ByteArray {
        val host = targetHost.trim()
        val ip = try { InetAddress.getByName(host) } catch (_: Exception) { null }
        return if (ip != null) {
            val addr = ip.address
            if (addr.size == 4) {
                val b = ByteArray(1 + 4)
                b[0] = 0x01.toByte() // IPv4
                System.arraycopy(addr, 0, b, 1, 4)
                b
            } else {
                val b = ByteArray(1 + 16)
                b[0] = 0x04.toByte() // IPv6 in SOCKS5
                System.arraycopy(addr, 0, b, 1, 16)
                b
            }
        } else {
            val domainBytes = host.toByteArray(Charsets.US_ASCII)
            val b = ByteArray(1 + 1 + domainBytes.size)
            b[0] = 0x03.toByte() // Domain in SOCKS5 / Trojan
            b[1] = domainBytes.size.toByte()
            System.arraycopy(domainBytes, 0, b, 2, domainBytes.size)
            b
        }
    }

    private fun parseAddress(targetHost: String): ByteArray {
        return parseAddressVless(targetHost)
    }

    private fun sha224Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-224")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun md5(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(data)
    }

    private fun fnv1a(data: ByteArray): Int {
        var hash = -2128831035 // 0x811C9DC5
        for (b in data) {
            hash = hash xor (b.toInt() and 0xFF)
            hash *= 16777619 // 0x01000193
        }
        return hash
    }

    private fun encryptAesCfb(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/CFB/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(data)
        } catch (_: Exception) {
            data
        }
    }

    private fun findSequence(data: ByteArray, length: Int, seq: ByteArray): Int {
        if (length < seq.size) return -1
        for (i in 0..length - seq.size) {
            var match = true
            for (j in seq.indices) {
                if (data[i + j] != seq[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }
}
