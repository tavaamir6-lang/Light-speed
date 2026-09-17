package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TrafficStat
import kotlinx.coroutines.flow.Flow

@Dao
interface TrafficStatsDao {
    @Query("SELECT * FROM traffic_stats ORDER BY timestamp DESC")
    fun getAllStats(): Flow<List<TrafficStat>>

    @Query("SELECT * FROM traffic_stats ORDER BY id DESC LIMIT :limit")
    fun getRecentStats(limit: Int = 30): Flow<List<TrafficStat>>

    @Query("SELECT dateString, '' as serverName, SUM(uploadBytes) as uploadBytes, SUM(downloadBytes) as downloadBytes, SUM(sessionDurationSeconds) as sessionDurationSeconds, MAX(timestamp) as timestamp, MIN(id) as id, NULL as serverId FROM traffic_stats GROUP BY dateString ORDER BY timestamp DESC LIMIT :days")
    fun getDailyAggregatedStats(days: Int = 14): Flow<List<TrafficStat>>

    @Query("SELECT serverName, dateString, SUM(uploadBytes) as uploadBytes, SUM(downloadBytes) as downloadBytes, SUM(sessionDurationSeconds) as sessionDurationSeconds, MAX(timestamp) as timestamp, MIN(id) as id, serverId FROM traffic_stats WHERE serverName != '' GROUP BY serverName ORDER BY (uploadBytes + downloadBytes) DESC")
    fun getServerAggregatedStats(): Flow<List<TrafficStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStat(stat: TrafficStat): Long

    @Query("DELETE FROM traffic_stats")
    suspend fun clearAll()
}
