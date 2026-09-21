package com.example.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborEntryIterator
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import java.net.NetworkInterface

class SingBoxPlatform(
    private val service: VpnService,
    private val context: Context,
    private val onTunEstablished: (ParcelFileDescriptor) -> Unit,
) : PlatformInterface {
    private var tun: ParcelFileDescriptor? = null
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var defaultNetworkCallback: ConnectivityManager.NetworkCallback? = null

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        check(service.protect(fd)) { "android: failed to protect core socket" }
    }

    override fun openTun(options: TunOptions): Int {
        check(VpnService.prepare(service) == null) { "android: missing VPN permission" }

        val builder = service.Builder()
            .setSession("Light Speed")
            .setMtu(options.mtu)

        val v4 = options.inet4Address
        while (v4.hasNext()) {
            val address = v4.next()
            builder.addAddress(address.address(), address.prefix())
        }

        val v6 = options.inet6Address
        while (v6.hasNext()) {
            val address = v6.next()
            builder.addAddress(address.address(), address.prefix())
        }

        if (options.autoRoute) {
            val r4 = options.inet4RouteAddress
            if (r4.hasNext()) {
                while (r4.hasNext()) {
                    val address = r4.next()
                    builder.addRoute(address.address(), address.prefix())
                }
            } else if (options.inet4Address.hasNext()) {
                builder.addRoute("0.0.0.0", 0)
            }

            val r6 = options.inet6RouteAddress
            if (r6.hasNext()) {
                while (r6.hasNext()) {
                    val address = r6.next()
                    builder.addRoute(address.address(), address.prefix())
                }
            } else if (options.inet6Address.hasNext()) {
                builder.addRoute("::", 0)
            }

            val dns = options.dnsServerAddress
            while (dns.hasNext()) builder.addDnsServer(dns.next())

            val include = options.includePackage
            while (include.hasNext()) {
                runCatching { builder.addAllowedApplication(include.next()) }
            }

            val exclude = options.excludePackage
            while (exclude.hasNext()) {
                runCatching { builder.addDisallowedApplication(exclude.next()) }
            }
        }

        tun?.close()
        tun = builder.establish() ?: error("android: failed to establish VPN")
        onTunEstablished(tun!!)
        return tun!!.fd
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner = ConnectionOwner().apply {
        userId = -1
        userName = ""
        setAndroidPackageNames(EmptyStringIterator)
    }

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        closeDefaultInterfaceMonitor(listener)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                notifyDefaultInterface(listener, network)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                notifyDefaultInterface(listener, network)
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: android.net.LinkProperties,
            ) {
                notifyDefaultInterface(listener, network)
            }

            override fun onLost(network: Network) {
                val current = connectivityManager.activeNetwork
                if (current != null) {
                    notifyDefaultInterface(listener, current)
                } else {
                    listener.updateDefaultInterface("", -1, false, false)
                }
            }
        }

        defaultNetworkCallback = callback
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }.onFailure {
            defaultNetworkCallback = null
            notifyCurrentDefaultInterface(listener)
        }

        notifyCurrentDefaultInterface(listener)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        defaultNetworkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
        }
        defaultNetworkCallback = null
    }

    private fun notifyCurrentDefaultInterface(listener: InterfaceUpdateListener) {
        connectivityManager.activeNetwork?.let { notifyDefaultInterface(listener, it) }
    }

    private fun notifyDefaultInterface(listener: InterfaceUpdateListener, network: Network) {
        val linkProperties = runCatching {
            connectivityManager.getLinkProperties(network)
        }.getOrNull() ?: return

        val interfaceName = linkProperties.interfaceName ?: return
        val interfaceIndex = runCatching {
            NetworkInterface.getByName(interfaceName)?.index ?: -1
        }.getOrDefault(-1)

        val capabilities = runCatching {
            connectivityManager.getNetworkCapabilities(network)
        }.getOrNull()

        val isExpensive = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_NOT_METERED
        ) == false

        listener.updateDefaultInterface(
            interfaceName,
            interfaceIndex,
            isExpensive,
            false,
        )
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val interfaces = mutableListOf<io.nekohasekai.libbox.NetworkInterface>()
        val networks = runCatching { connectivityManager.allNetworks }.getOrDefault(emptyArray())
        val javaInterfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull()

        while (javaInterfaces?.hasMoreElements() == true) {
            val ni = javaInterfaces.nextElement()
            if (!ni.isUp) continue

            val item = io.nekohasekai.libbox.NetworkInterface().apply {
                name = ni.name
                index = ni.index
                mtu = runCatching { ni.mtu }.getOrDefault(0)
                addresses = StringListIterator(
                    ni.interfaceAddresses.map { it.address.hostAddress.orEmpty() + "/" + it.networkPrefixLength }
                )
                flags = 0
                type = 0
                dnsServer = EmptyStringIterator
                gateway = EmptyStringIterator
                metered = false
            }

            val matchingNetwork = networks.firstOrNull { network ->
                connectivityManager.getLinkProperties(network)?.interfaceName == ni.name
            }
            if (matchingNetwork != null) {
                val lp = connectivityManager.getLinkProperties(matchingNetwork)
                val nc = connectivityManager.getNetworkCapabilities(matchingNetwork)

                item.dnsServer = StringListIterator(
                    lp?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty()
                )
                item.gateway = StringListIterator(
                    lp?.routes
                        ?.filter { it.destination.prefixLength == 0 }
                        ?.mapNotNull { it.gateway?.hostAddress }
                        ?.filter { it.isNotBlank() }
                        .orEmpty()
                )
                item.metered = nc?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                ) == false
                item.type = when {
                    nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 1
                    nc?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> 2
                    nc?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> 3
                    else -> 0
                }
            }

            interfaces += item
        }

        return object : NetworkInterfaceIterator {
            private var index = 0

            override fun hasNext(): Boolean = index < interfaces.size

            override fun next(): io.nekohasekai.libbox.NetworkInterface = interfaces[index++]
        }
    }

    override fun underNetworkExtension(): Boolean = false
    override fun includeAllNetworks(): Boolean = false
    override fun readWIFIState(): WIFIState? = null
    override fun clearDNSCache() = Unit
    override fun sendNotification(notification: Notification) = Unit
    override fun cancelNotification(identifier: String, typeID: Int) = Unit
    override fun startNeighborMonitor(listener: NeighborUpdateListener?) = Unit
    override fun closeNeighborMonitor(listener: NeighborUpdateListener?) = Unit
    override fun registerMyInterface(name: String?) = Unit
    override fun usePlatformShell(): Boolean = false
    override fun checkPlatformShell() = error("platform shell is not available")

    override fun openShellSession(
        user: PlatformUser?,
        command: String?,
        environ: StringIterator?,
        term: String?,
        rows: Int,
        cols: Int,
    ): ShellSession = error("platform shell is not available")

    override fun lookupUser(username: String?): PlatformUser = PlatformUser().apply {
        this.username = username ?: ""
        uid = android.os.Process.myUid()
        gid = android.os.Process.myUid()
        homeDir = context.filesDir.absolutePath
    }

    override fun lookupSFTPServer(): String = ""
    override fun readSystemSSHHostKey(): String = ""
    override fun tailscaleHostname(): String = "Light Speed"
    override fun usePlatformBridge(): Boolean = false
    override fun createBridge(options: BridgeOptions?): BridgeSession =
        error("bridge requires root")

    fun close() {
        closeDefaultInterfaceMonitor(EmptyInterfaceUpdateListener)
        tun?.close()
        tun = null
    }

    private object EmptyInterfaceUpdateListener : InterfaceUpdateListener {
        override fun updateDefaultInterface(
            interfaceName: String,
            interfaceIndex: Int,
            isExpensive: Boolean,
            isConstrained: Boolean,
        ) = Unit
    }

    private object EmptyStringIterator : StringIterator {
        override fun len(): Int = 0
        override fun hasNext(): Boolean = false
        override fun next(): String = ""
    }

    private class StringListIterator(
        private val values: List<String>,
    ) : StringIterator {
        private var index = 0

        override fun len(): Int = values.size
        override fun hasNext(): Boolean = index < values.size
        override fun next(): String = values[index++]
    }
}
