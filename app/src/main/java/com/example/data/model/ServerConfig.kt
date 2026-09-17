package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: Long? = null,
    val name: String,
    val protocol: ProtocolType,
    val address: String,
    val port: Int,
    val uuidOrPassword: String,
    val alterId: Int = 0,
    val security: String = "auto",
    val networkType: String = "tcp", // tcp, ws, grpc, h2
    val headerType: String = "none",
    val host: String = "",
    val path: String = "",
    val tls: String = "none", // none, tls, reality
    val sni: String = "",
    val alpn: String = "",
    val flow: String = "",
    val publicKey: String = "", // for reality
    val shortId: String = "", // for reality
    val rawUri: String = "",
    val pingMs: Int = -1, // -1 = untested, -2 = timeout/error
    val isSelected: Boolean = false,
    val lastTestedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
