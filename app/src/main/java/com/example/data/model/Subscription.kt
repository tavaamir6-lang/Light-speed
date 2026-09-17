package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val autoUpdateHours: Int = 12,
    val lastUpdated: Long = 0L,
    val serverCount: Int = 0,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
