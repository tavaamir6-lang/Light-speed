package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY isSelected DESC, id ASC")
    fun getAllServers(): Flow<List<ServerConfig>>

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    fun getSelectedServer(): Flow<ServerConfig?>

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedServerSync(): ServerConfig?

    @Query("SELECT * FROM servers WHERE id = :id")
    fun getServerById(id: Long): Flow<ServerConfig?>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerByIdSync(id: Long): ServerConfig?

    @Query("SELECT * FROM servers WHERE subscriptionId = :subId")
    suspend fun getServersBySubscriptionId(subId: Long): List<ServerConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerConfig>): List<Long>

    @Update
    suspend fun updateServer(server: ServerConfig)

    @Query("UPDATE servers SET pingMs = :pingMs, lastTestedAt = :testedAt WHERE id = :id")
    suspend fun updatePing(id: Long, pingMs: Int, testedAt: Long)

    @Query("UPDATE servers SET isSelected = CASE WHEN id = :selectedId THEN 1 ELSE 0 END")
    suspend fun setSelectedServer(selectedId: Long)

    @Query("SELECT * FROM servers WHERE pingMs > 0 ORDER BY pingMs ASC LIMIT 1")
    suspend fun getLowestPingServer(): ServerConfig?

    @Delete
    suspend fun deleteServer(server: ServerConfig)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)

    @Query("DELETE FROM servers WHERE subscriptionId = :subId")
    suspend fun deleteBySubscriptionId(subId: Long)

    @Query("DELETE FROM servers")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceSubscriptionServers(subId: Long, newServers: List<ServerConfig>) {
        deleteBySubscriptionId(subId)
        insertServers(newServers)
    }
}
