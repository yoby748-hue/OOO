package com.example.vpn.v2ray

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.example.dns.DnsPacketUtils
import com.example.dns.DnsVpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class V2RayTunnelEngine(
    private val vpnService: VpnService,
    private val config: V2RayConfig,
    private val pfd: ParcelFileDescriptor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    private val handler = V2RayProtocolHandler(vpnService, config)

    private val inputStream = FileInputStream(pfd.fileDescriptor)
    private val outputStream = FileOutputStream(pfd.fileDescriptor)
    private val writeLock = Any()

    private var workerThread: Thread? = null

    // Track active TCP connections: key = "clientIp:clientPort->destIp:destPort"
    private data class TcpStream(
        val clientIp: InetAddress,
        val clientPort: Int,
        val destIp: InetAddress,
        val destPort: Int,
        var clientSeq: Long,
        var serverSeq: Long,
        var isEstablished: Boolean = false,
        var isClosed: Boolean = false,
        var proxyConn: V2RayProtocolHandler.ProxyConnection? = null,
        val pendingPayloads: java.util.concurrent.ConcurrentLinkedQueue<ByteArray> = java.util.concurrent.ConcurrentLinkedQueue()
    )

    private val tcpStreams = ConcurrentHashMap<String, TcpStream>()

    fun start() {
        if (isRunning.getAndSet(true)) return

        workerThread = Thread {
            val buffer = ByteArray(32767)
            try {
                while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                    val length = inputStream.read(buffer)
                    if (length <= 0) break

                    val packet = try {
                        IpPacket(buffer, length)
                    } catch (_: Exception) {
                        continue
                    }

                    if (packet.isIpv4) {
                        if (packet.isTcp) {
                            handleTcpPacket(packet)
                        } else if (packet.isUdp) {
                            handleUdpPacket(packet)
                        }
                    }
                }
            } catch (_: Exception) {
                // Tun closed
            } finally {
                stop()
            }
        }.apply {
            name = "V2RayTunnelWorker"
            start()
        }
    }

    private fun handleTcpPacket(packet: IpPacket) {
        val streamKey = "${packet.sourceIp.hostAddress}:${packet.sourcePort}->${packet.destIp.hostAddress}:${packet.destPort}"

        if (packet.tcpSyn && !packet.tcpAck) {
            // New TCP connection handshake
            val initialServerSeq = (System.currentTimeMillis() and 0x7FFFFFFF)
            val stream = TcpStream(
                clientIp = packet.sourceIp,
                clientPort = packet.sourcePort,
                destIp = packet.destIp,
                destPort = packet.destPort,
                clientSeq = packet.tcpSeqNum + 1,
                serverSeq = initialServerSeq + 1
            )
            tcpStreams[streamKey] = stream

            // Send SYN-ACK back to client immediately so OS TCP stack connects
            val synAck = IpPacket.buildTcpPacket(
                srcIp = packet.destIp,
                dstIp = packet.sourceIp,
                srcPort = packet.destPort,
                dstPort = packet.sourcePort,
                seqNum = initialServerSeq,
                ackNum = stream.clientSeq,
                syn = true,
                ack = true
            )
            writeToTun(synAck)

            // Open V2Ray proxy connection in background
            scope.launch {
                try {
                    val targetHost = packet.destIp.hostAddress ?: ""
                    val targetPort = packet.destPort
                    val proxy = handler.openConnection(
                        targetHost = targetHost,
                        targetPort = targetPort,
                        isUdp = false,
                        timeoutMs = 12000
                    )
                    stream.proxyConn = proxy
                    stream.isEstablished = true
                    flushPendingPayloads(stream, proxy)
                    startProxyReader(streamKey, stream, proxy)
                } catch (_: Exception) {
                    closeTcpStream(streamKey, sendRst = true)
                }
            }
            return
        }

        val stream = tcpStreams[streamKey] ?: return

        if (packet.tcpRst) {
            closeTcpStream(streamKey, sendRst = false)
            return
        }

        if (packet.tcpFin) {
            // Client closed connection
            stream.clientSeq = packet.tcpSeqNum + 1
            val finAck = IpPacket.buildTcpPacket(
                srcIp = stream.destIp,
                dstIp = stream.clientIp,
                srcPort = stream.destPort,
                dstPort = stream.clientPort,
                seqNum = stream.serverSeq,
                ackNum = stream.clientSeq,
                ack = true,
                fin = true
            )
            writeToTun(finAck)
            closeTcpStream(streamKey, sendRst = false)
            return
        }

        if (packet.payloadLength > 0) {
            val payload = packet.getPayload()
            stream.clientSeq = packet.tcpSeqNum + packet.payloadLength

            // Send ACK packet back to client
            val ackPacket = IpPacket.buildTcpPacket(
                srcIp = stream.destIp,
                dstIp = stream.clientIp,
                srcPort = stream.destPort,
                dstPort = stream.clientPort,
                seqNum = stream.serverSeq,
                ackNum = stream.clientSeq,
                ack = true
            )
            writeToTun(ackPacket)

            // Buffer payload and forward to remote V2Ray server
            stream.pendingPayloads.add(payload)

            scope.launch {
                try {
                    val proxy = stream.proxyConn
                    if (proxy != null && !stream.isClosed) {
                        flushPendingPayloads(stream, proxy)
                    }
                } catch (_: Exception) {
                    closeTcpStream(streamKey, sendRst = true)
                }
            }
        }
    }

    private fun flushPendingPayloads(stream: TcpStream, proxy: V2RayProtocolHandler.ProxyConnection) {
        synchronized(proxy.outputStream) {
            while (!stream.pendingPayloads.isEmpty() && !stream.isClosed) {
                val chunk = stream.pendingPayloads.poll() ?: break
                handler.writeData(proxy.outputStream, chunk, proxy.isWebSocket, proxy.protocol)
            }
        }
    }

    private fun startProxyReader(streamKey: String, stream: TcpStream, proxy: V2RayProtocolHandler.ProxyConnection) {
        scope.launch {
            val readBuffer = ByteArray(16384)
            val maxMss = 1400
            try {
                while (isRunning.get() && !stream.isClosed && isActive) {
                    val bytesRead = handler.readData(proxy, readBuffer)
                    if (bytesRead <= 0) break

                    var offset = 0
                    while (offset < bytesRead && !stream.isClosed && isRunning.get()) {
                        val chunkSize = (bytesRead - offset).coerceAtMost(maxMss)
                        val responseChunk = ByteArray(chunkSize)
                        System.arraycopy(readBuffer, offset, responseChunk, 0, chunkSize)

                        val tcpPacket = IpPacket.buildTcpPacket(
                            srcIp = stream.destIp,
                            dstIp = stream.clientIp,
                            srcPort = stream.destPort,
                            dstPort = stream.clientPort,
                            seqNum = stream.serverSeq,
                            ackNum = stream.clientSeq,
                            ack = true,
                            payload = responseChunk
                        )
                        writeToTun(tcpPacket)
                        stream.serverSeq += chunkSize
                        offset += chunkSize
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    if (!stream.isClosed) {
                        val finPacket = IpPacket.buildTcpPacket(
                            srcIp = stream.destIp,
                            dstIp = stream.clientIp,
                            srcPort = stream.destPort,
                            dstPort = stream.clientPort,
                            seqNum = stream.serverSeq,
                            ackNum = stream.clientSeq,
                            ack = true,
                            fin = true
                        )
                        writeToTun(finPacket)
                    }
                } catch (_: Exception) {}
                closeTcpStream(streamKey, sendRst = false)
            }
        }
    }

    private fun closeTcpStream(streamKey: String, sendRst: Boolean) {
        val stream = tcpStreams.remove(streamKey) ?: return
        stream.isClosed = true
        try { stream.proxyConn?.close() } catch (_: Exception) {}

        if (sendRst) {
            val rstPacket = IpPacket.buildTcpPacket(
                srcIp = stream.destIp,
                dstIp = stream.clientIp,
                srcPort = stream.destPort,
                dstPort = stream.clientPort,
                seqNum = stream.serverSeq,
                ackNum = stream.clientSeq,
                rst = true
            )
            writeToTun(rstPacket)
        }
    }

    private fun handleUdpPacket(packet: IpPacket) {
        val payload = packet.getPayload()
        if (payload.isEmpty()) return

        // Reject QUIC (UDP 443) with ICMP Port Unreachable so YouTube & Instagram
        // instantly switch to TCP HTTPS without waiting for UDP timeouts
        if (packet.destPort == 443) {
            val icmp = IpPacket.buildIcmpPortUnreachable(packet)
            if (icmp.isNotEmpty()) {
                writeToTun(icmp)
            }
            return
        }

        // If it's DNS query (port 53), resolve via DNS forwarder and send back
        if (packet.destPort == 53) {
            scope.launch {
                try {
                    val dnsResponse = resolveDnsQuery(payload)
                    if (dnsResponse != null && dnsResponse.isNotEmpty()) {
                        val udpPacket = IpPacket.buildUdpPacket(
                            srcIp = packet.destIp,
                            dstIp = packet.sourceIp,
                            srcPort = packet.destPort,
                            dstPort = packet.sourcePort,
                            payload = dnsResponse
                        )
                        writeToTun(udpPacket)
                        DnsVpnState.incrementQueries()
                    }
                } catch (_: Exception) {}
            }
            return
        }

        // Generic UDP over protected socket
        scope.launch {
            try {
                val udpSocket = DatagramSocket()
                vpnService.protect(udpSocket)
                udpSocket.soTimeout = 4000

                val sendPacket = DatagramPacket(payload, payload.size, packet.destIp, packet.destPort)
                udpSocket.send(sendPacket)

                val recvBuf = ByteArray(4096)
                val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
                udpSocket.receive(recvPacket)

                val replyData = ByteArray(recvPacket.length)
                System.arraycopy(recvBuf, 0, replyData, 0, recvPacket.length)

                val returnPacket = IpPacket.buildUdpPacket(
                    srcIp = packet.destIp,
                    dstIp = packet.sourceIp,
                    srcPort = packet.destPort,
                    dstPort = packet.sourcePort,
                    payload = replyData
                )
                writeToTun(returnPacket)
                udpSocket.close()
            } catch (_: Exception) {}
        }
    }

    private fun resolveDnsViaProxy(queryPayload: ByteArray): ByteArray? {
        val dnsServers = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")
        for (dnsServer in dnsServers) {
            var proxy: V2RayProtocolHandler.ProxyConnection? = null
            try {
                proxy = handler.openConnection(
                    targetHost = dnsServer,
                    targetPort = 53,
                    isUdp = false,
                    timeoutMs = 3500
                )
                val outStream = proxy.outputStream

                // Send TCP DNS query: 2 bytes length (Big Endian) + DNS payload
                val queryBuffer = java.nio.ByteBuffer.allocate(2 + queryPayload.size)
                queryBuffer.putShort(queryPayload.size.toShort())
                queryBuffer.put(queryPayload)

                handler.writeData(outStream, queryBuffer.array(), proxy.isWebSocket, proxy.protocol)

                // Read TCP DNS response: 2 bytes length + response bytes
                val respBuf = ByteArray(4096)
                val bytesRead = handler.readData(proxy, respBuf)
                if (bytesRead >= 2) {
                    val len = ((respBuf[0].toInt() and 0xFF) shl 8) or (respBuf[1].toInt() and 0xFF)
                    if (len in 12..4094 && bytesRead >= 2 + len) {
                        val dnsResponse = ByteArray(len)
                        System.arraycopy(respBuf, 2, dnsResponse, 0, len)
                        return dnsResponse
                    } else if (bytesRead > 2) {
                        val dnsResponse = ByteArray(bytesRead - 2)
                        System.arraycopy(respBuf, 2, dnsResponse, 0, bytesRead - 2)
                        return dnsResponse
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { proxy?.close() } catch (_: Exception) {}
            }
        }
        return null
    }

    private val dnsCache = ConcurrentHashMap<String, ByteArray>()

    private fun resolveDnsQuery(queryPayload: ByteArray): ByteArray? {
        if (queryPayload.size < 12) return null

        // Cache key based on DNS question (excluding transaction ID in bytes 0..1)
        val questionKey = queryPayload.sliceArray(2 until queryPayload.size).contentToString()
        val cached = dnsCache[questionKey]
        if (cached != null) {
            val response = cached.copyOf()
            response[0] = queryPayload[0]
            response[1] = queryPayload[1]
            return response
        }

        // 1. Fast DNS-over-HTTPS (DoH) over protected TLS (bypasses all ISP UDP 53 DNS poisoning)
        val dohEndpoints = listOf(
            "https://1.1.1.1/dns-query",
            "https://8.8.8.8/dns-query",
            "https://9.9.9.9/dns-query",
            "https://1.0.0.1/dns-query"
        )
        for (endpoint in dohEndpoints) {
            try {
                val url = java.net.URL(endpoint)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/dns-message")
                conn.setRequestProperty("Accept", "application/dns-message")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

                conn.outputStream.use { os ->
                    os.write(queryPayload)
                    os.flush()
                }

                if (conn.responseCode == 200) {
                    val responseBytes = conn.inputStream.use { it.readBytes() }
                    if (responseBytes.size >= 12) {
                        dnsCache[questionKey] = responseBytes
                        return responseBytes
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Direct UDP DNS queries over protected socket
        val dnsServers = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "1.0.0.1")
        for (dnsIp in dnsServers) {
            try {
                val socket = DatagramSocket()
                vpnService.protect(socket)
                socket.soTimeout = 1500

                val addr = InetAddress.getByName(dnsIp)
                val outPacket = DatagramPacket(queryPayload, queryPayload.size, addr, 53)
                socket.send(outPacket)

                val inBuffer = ByteArray(4096)
                val inPacket = DatagramPacket(inBuffer, inBuffer.size)
                socket.receive(inPacket)

                val result = ByteArray(inPacket.length)
                System.arraycopy(inBuffer, 0, result, 0, inPacket.length)
                socket.close()
                if (result.size >= 12) {
                    dnsCache[questionKey] = result
                    return result
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Proxy TCP DNS query
        val proxyResult = resolveDnsViaProxy(queryPayload)
        if (proxyResult != null && proxyResult.size >= 12) {
            dnsCache[questionKey] = proxyResult
            return proxyResult
        }

        return null
    }

    private fun writeToTun(data: ByteArray) {
        try {
            synchronized(writeLock) {
                outputStream.write(data)
                outputStream.flush()
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        workerThread?.interrupt()
        workerThread = null

        tcpStreams.keys.toList().forEach { key ->
            closeTcpStream(key, sendRst = false)
        }
        tcpStreams.clear()

        scope.cancel()
        try { inputStream.close() } catch (_: Exception) {}
        try { outputStream.close() } catch (_: Exception) {}
    }
}
