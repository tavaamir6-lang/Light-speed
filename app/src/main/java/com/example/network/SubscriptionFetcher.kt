package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object SubscriptionFetcher {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Downloads subscription content from the given URL.
     * Uses standard V2Ray/Xray User-Agent to ensure subscription servers respond correctly.
     */
    suspend fun fetchSubscription(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url.trim())
                .header("User-Agent", "v2rayNG/1.8.5 (Linux; Android; Client)")
                .header("Accept", "*/*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(IOException("Subscription response body is empty"))
            }

            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
