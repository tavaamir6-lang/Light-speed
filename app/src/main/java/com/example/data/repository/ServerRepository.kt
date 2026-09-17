package com.example.data.repository

import com.example.data.db.ServerDao
import com.example.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow

class ServerRepository(private val serverDao: ServerDao) {

    val allServers: Flow<List<ServerConfig>> = serverDao.getAllServers()
    val selectedServer: Flow<ServerConfig?> = serverDao.getSelectedServer()

    suspend fun getSelectedServerSync(): ServerConfig? = serverDao.getSelectedServerSync()

    suspend fun insertServer(server: ServerConfig): Long = serverDao.insertServer(server)

    suspend fun insertServers(servers: List<ServerConfig>): List<Long> = serverDao.insertServers(servers)

    suspend fun updateServer(server: ServerConfig) = serverDao.updateServer(server)

    suspend fun updatePing(id: Long, pingMs: Int, testedAt: Long = System.currentTimeMillis()) {
        serverDao.updatePing(id, pingMs, testedAt)
    }

    suspend fun selectServer(id: Long) = serverDao.setSelectedServer(id)

    suspend fun getLowestPingServer(): ServerConfig? = serverDao.getLowestPingServer()

    suspend fun deleteServer(server: ServerConfig) = serverDao.deleteServer(server)

    suspend fun deleteServerById(id: Long) = serverDao.deleteServerById(id)

    suspend fun deleteBySubscriptionId(subId: Long) = serverDao.deleteBySubscriptionId(subId)

    suspend fun clearAll() = serverDao.clearAll()
}
