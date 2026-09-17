package com.example.data.repository

import com.example.data.db.TrafficStatsDao
import com.example.data.model.TrafficStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrafficRepository(private val trafficStatsDao: TrafficStatsDao) {

    val dailyStats: Flow<List<TrafficStat>> = trafficStatsDao.getDailyAggregatedStats(14)
    val serverStats: Flow<List<TrafficStat>> = trafficStatsDao.getServerAggregatedStats()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun recordSessionTraffic(
        serverId: Long?,
        serverName: String,
        uploadBytes: Long,
        downloadBytes: Long,
        durationSeconds: Long
    ) = withContext(Dispatchers.IO) {
        if (uploadBytes <= 0 && downloadBytes <= 0) return@withContext

        val today = dateFormat.format(Date())
        val stat = TrafficStat(
            dateString = today,
            serverId = serverId,
            serverName = serverName,
            uploadBytes = uploadBytes,
            downloadBytes = downloadBytes,
            sessionDurationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis()
        )
        trafficStatsDao.insertStat(stat)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        trafficStatsDao.clearAll()
    }
}
