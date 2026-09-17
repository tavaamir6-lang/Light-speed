package com.example.data.repository

import com.example.data.db.ServerDao
import com.example.data.db.SubscriptionDao
import com.example.data.model.Subscription
import com.example.data.parser.V2RayParser
import com.example.network.SubscriptionFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val serverDao: ServerDao
) {
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

    suspend fun addSubscription(title: String, url: String, autoUpdateHours: Int = 12): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val sub = Subscription(
                title = title.ifBlank { "Subscription" },
                url = url.trim(),
                autoUpdateHours = autoUpdateHours
            )
            val subId = subscriptionDao.insertSubscription(sub)

            // Auto-fetch immediately
            refreshSubscription(subId)
            Result.success(subId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSubscription(id: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val sub = subscriptionDao.getSubscriptionByIdSync(id)
                ?: return@withContext Result.failure(IllegalArgumentException("Subscription not found"))

            val fetchResult = SubscriptionFetcher.fetchSubscription(sub.url)
            if (fetchResult.isFailure) {
                return@withContext Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch"))
            }

            val body = fetchResult.getOrThrow()
            val parsedServers = V2RayParser.parseSubscriptionContent(body, subscriptionId = id)

            serverDao.replaceSubscriptionServers(id, parsedServers)
            subscriptionDao.updateServerCountAndTimestamp(id, parsedServers.size, System.currentTimeMillis())

            // If no server is selected, select the first server
            val currentSelected = serverDao.getSelectedServerSync()
            if (currentSelected == null && parsedServers.isNotEmpty()) {
                val firstServer = serverDao.getServersBySubscriptionId(id).firstOrNull()
                if (firstServer != null) {
                    serverDao.setSelectedServer(firstServer.id)
                }
            }

            Result.success(parsedServers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAllSubscriptions(): Map<Long, Result<Int>> = withContext(Dispatchers.IO) {
        val enabledSubs = subscriptionDao.getEnabledSubscriptions()
        val results = mutableMapOf<Long, Result<Int>>()
        for (sub in enabledSubs) {
            results[sub.id] = refreshSubscription(sub.id)
        }
        results
    }

    suspend fun deleteSubscription(id: Long) = withContext(Dispatchers.IO) {
        serverDao.deleteBySubscriptionId(id)
        subscriptionDao.deleteSubscriptionById(id)
    }

    suspend fun updateSubscription(subscription: Subscription) = withContext(Dispatchers.IO) {
        subscriptionDao.updateSubscription(subscription)
    }
}
