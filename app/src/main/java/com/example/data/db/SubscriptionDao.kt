package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun getSubscriptionById(id: Long): Flow<Subscription?>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionByIdSync(id: Long): Subscription?

    @Query("SELECT * FROM subscriptions WHERE enabled = 1")
    suspend fun getEnabledSubscriptions(): List<Subscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Update
    suspend fun updateSubscription(subscription: Subscription)

    @Query("UPDATE subscriptions SET serverCount = :count, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateServerCountAndTimestamp(id: Long, count: Int, timestamp: Long)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)
}
