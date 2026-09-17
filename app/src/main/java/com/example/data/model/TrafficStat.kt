package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic_stats")
data class TrafficStat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String, // e.g. "2026-09-16"
    val serverId: Long? = null,
    val serverName: String = "",
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
