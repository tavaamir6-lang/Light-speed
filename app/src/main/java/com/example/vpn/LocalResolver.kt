package com.example.vpn

import android.net.DnsResolver
import android.os.Build
import android.os.CancellationSignal
import android.system.ErrnoException
import androidx.annotation.RequiresApi
import io.nekohasekai.libbox.ExchangeContext
import io.nekohasekai.libbox.LocalDNSTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LocalResolver : LocalDNSTransport {
    private const val RCODE_NXDOMAIN = 3

    override fun raw(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun exchange(ctx: ExchangeContext, message: ByteArray) {
        runBlocking {
            val network = DefaultNetworkHolder.require()
            suspendCoroutine { continuation ->
                val signal = CancellationSignal()
                ctx.onCancel { signal.cancel() }
                val callback = object : DnsResolver.Callback<ByteArray> {
                    override fun onAnswer(answer: ByteArray, rcode: Int) {
                        if (rcode == 0) ctx.rawSuccess(answer) else ctx.errorCode(rcode)
                        continuation.resume(Unit)
                    }

                    override fun onError(error: DnsResolver.DnsException) {
                        val cause = error.cause
                        if (cause is ErrnoException) {
                            ctx.errnoCode(cause.errno)
                            continuation.resume(Unit)
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                }
                DnsResolver.getInstance().rawQuery(
                    network,
                    message,
                    DnsResolver.FLAG_NO_RETRY,
                    Dispatchers.IO.asExecutor(),
                    signal,
                    callback,
                )
            }
        }
    }

    override fun lookup(ctx: ExchangeContext, network: String, domain: String) {
        runBlocking {
            val defaultNetwork = DefaultNetworkHolder.require()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                suspendCoroutine { continuation ->
                    val signal = CancellationSignal()
                    ctx.onCancel { signal.cancel() }
                    val callback = object : DnsResolver.Callback<Collection<InetAddress>> {
                        override fun onAnswer(answer: Collection<InetAddress>, rcode: Int) {
                            if (rcode == 0) {
                                ctx.success(answer.joinToString("\n") { it.hostAddress.orEmpty() })
                            } else {
                                ctx.errorCode(rcode)
                            }
                            continuation.resume(Unit)
                        }

                        override fun onError(error: DnsResolver.DnsException) {
                            val cause = error.cause
                            if (cause is ErrnoException) ctx.errnoCode(cause.errno)
                            continuation.resume(Unit)
                        }
                    }
                    val type = when {
                        network.endsWith("4") -> DnsResolver.TYPE_A
                        network.endsWith("6") -> DnsResolver.TYPE_AAAA
                        else -> null
                    }
                    if (type != null) {
                        DnsResolver.getInstance().query(
                            defaultNetwork, domain, type, DnsResolver.FLAG_NO_RETRY,
                            Dispatchers.IO.asExecutor(), signal, callback
                        )
                    } else {
                        DnsResolver.getInstance().query(
                            defaultNetwork, domain, DnsResolver.FLAG_NO_RETRY,
                            Dispatchers.IO.asExecutor(), signal, callback
                        )
                    }
                }
            } else {
                val answer = try {
                    defaultNetwork.getAllByName(domain)
                } catch (_: UnknownHostException) {
                    ctx.errorCode(RCODE_NXDOMAIN)
                    return@runBlocking
                }
                ctx.success(answer.joinToString("\n") { it.hostAddress.orEmpty() })
            }
        }
    }
}

object DefaultNetworkHolder {
    private val lock = Any()
    @Volatile private var network: android.net.Network? = null
    private var callback: android.net.ConnectivityManager.NetworkCallback? = null

    fun start(context: android.content.Context) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        synchronized(lock) {
            if (callback != null) return
            network = cm.activeNetwork
            val cb = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(n: android.net.Network) {
                    network = n
                }
                override fun onCapabilitiesChanged(n: android.net.Network, caps: android.net.NetworkCapabilities) {
                    network = n
                }
                override fun onLost(n: android.net.Network) {
                    network = cm.activeNetwork
                }
            }
            callback = cb
            val request = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    cm.registerBestMatchingNetworkCallback(request, cb, null)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cm.requestNetwork(request, cb)
                } else {
                    cm.registerDefaultNetworkCallback(cb)
                }
            }
        }
    }

    fun stop(context: android.content.Context) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        synchronized(lock) {
            callback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
            callback = null
            network = null
        }
    }

    fun require(): android.net.Network =
        network ?: error("missing default network")
}
