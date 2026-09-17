package com.example.network

import com.example.data.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

object PingTester {

    /**
     * Tests real TCP latency to server host & port
     * Returns: latency in ms, or -2 on timeout/error
     */
    suspend fun testTcpPing(server: ServerConfig, timeoutMs: Int = 3000): Int = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            val address = InetSocketAddress(server.address, server.port)
            val elapsed = measureTimeMillis {
                socket = Socket()
                socket?.connect(address, timeoutMs)
            }
            elapsed.toInt()
        } catch (e: Exception) {
            -2 // error or timeout
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Tests HTTP delay (generate_204)
     */
    suspend fun testHttpPing(url: String, timeoutMs: Int = 4000): Int = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.instanceFollowRedirects = false
            connection.requestMethod = "HEAD"
            connection.connect()
            val code = connection.responseCode
            connection.disconnect()
            if (code in 200..399) {
                (System.currentTimeMillis() - start).toInt()
            } else {
                -2
            }
        } catch (e: Exception) {
            -2
        }
    }
}
