package com.example.data

import com.example.data.model.ProtocolType
import com.example.data.model.ServerConfig
import com.example.data.model.Subscription

object SampleData {
    val defaultSubscriptions = listOf(
        Subscription(
            id = 1,
            title = "Iran LightSpeed 🇹🇷",
            url = "https://orginal.iranlightspeed.xyz:2096/sub/w63xmya59lum3y8n",
            autoUpdateHours = 6,
            lastUpdated = System.currentTimeMillis(),
            serverCount = 2
        )
    )

    val defaultServers = listOf(
        ServerConfig(
            id = 1,
            subscriptionId = 1,
            name = "Lightspeed Tunnel 🇹🇷",
            protocol = ProtocolType.VLESS,
            address = "orginal.iranlightspeed.xyz",
            port = 2086,
            uuidOrPassword = "baa07c76-303d-4939-85fe-a09872748133",
            networkType = "tcp",
            tls = "tls",
            sni = "shop.iranlightspeed.ir",
            alpn = "h2,http/1.1,h3",
            pingMs = 92,
            isSelected = true,
            lastTestedAt = System.currentTimeMillis(),
            rawUri = "vless://baa07c76-303d-4939-85fe-a09872748133@orginal.iranlightspeed.xyz:2086?alpn=h2%2Chttp%2F1.1%2Ch3&ech=AGn%2BDQBlAAAgACCgDFeVML8LW21i6%2FX0Wjgz7G7%2BWPhEkHPzQRxCRolWWQAkAAEAAQABAAIAAQADAAIAAQACAAIAAgADAAMAAQADAAIAAwADABZzaG9wLmlyYW5saWdodHNwZWVkLmlyAAA%3D&encryption=mlkem768x25519plus.native.0rtt.9OuKoGZUD47XEM2yj6X6qhPm1x_Qq6rjX53gN2Bv8gs&fp=chrome&security=tls&sni=shop.iranlightspeed.ir&type=tcp#Lightspeed%20Tunnel%20%F0%9F%87%B9%F0%9F%87%B7"
        ),
        ServerConfig(
            id = 2,
            subscriptionId = 1,
            name = "📥 153.60MB (Active Status)",
            protocol = ProtocolType.VLESS,
            address = "chom.clasmateso.tr",
            port = 443,
            uuidOrPassword = "baa07c76-303d-4939-85fe-a09872748133",
            networkType = "ws",
            path = "/xvpnws/",
            tls = "tls",
            sni = "osmeiti.app.runonflux.io",
            alpn = "h2",
            pingMs = 1076,
            isSelected = false,
            lastTestedAt = System.currentTimeMillis(),
            rawUri = "vless://baa07c76-303d-4939-85fe-a09872748133@chom.clasmateso.tr:443?alpn=h2&encryption=none&fp=chrome&host=&path=%2Fxvpnws%2F&security=tls&sni=osmeiti.app.runonflux.io&type=ws#%F0%9F%93%A5153.60MB%20%E2%95%91%20%F0%9F%93%8A%200.0%25%20%E2%95%91%20%E2%8F%B0%202D%20%E2%95%91%20%E2%9C%85"
        ),
        ServerConfig(
            id = 3,
            subscriptionId = null,
            name = "🇩🇪 Germany - Frankfurt Fast",
            protocol = ProtocolType.VLESS,
            address = "de-fra01.xraynode.net",
            port = 443,
            uuidOrPassword = "7f8b9e10-3d2a-4a5b-8c1d-9e0f1a2b3c4d",
            networkType = "ws",
            path = "/vless-ws",
            tls = "tls",
            sni = "de-fra01.xraynode.net",
            pingMs = 74,
            isSelected = false,
            lastTestedAt = System.currentTimeMillis() - 120_000,
            rawUri = "vless://7f8b9e10-3d2a-4a5b-8c1d-9e0f1a2b3c4d@de-fra01.xraynode.net:443?type=ws&security=tls&path=%2Fvless-ws&sni=de-fra01.xraynode.net#%F0%9F%87%A9%F0%9F%87%AA%20Germany%20-%20Frankfurt%20Fast"
        ),
        ServerConfig(
            id = 4,
            subscriptionId = null,
            name = "🇫🇮 Finland - Helsinki Reality",
            protocol = ProtocolType.VLESS,
            address = "fi-hel01.xraynode.net",
            port = 443,
            uuidOrPassword = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
            networkType = "tcp",
            tls = "reality",
            sni = "www.microsoft.com",
            flow = "xtls-rprx-vision",
            publicKey = "abcdef0123456789abcdef0123456789",
            shortId = "1a2b3c4d",
            pingMs = 92,
            isSelected = false,
            lastTestedAt = System.currentTimeMillis() - 180_000,
            rawUri = "vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@fi-hel01.xraynode.net:443?security=reality&sni=www.microsoft.com&pbk=abcdef0123456789abcdef0123456789&sid=1a2b3c4d&flow=xtls-rprx-vision#%F0%9F%87%AB%F0%9F%87%AE%20Finland%20-%20Helsinki%20Reality"
        ),
        ServerConfig(
            id = 3,
            subscriptionId = 1,
            name = "🇳🇱 Netherlands - Amsterdam Stream",
            protocol = ProtocolType.VMESS,
            address = "nl-ams02.xraynode.net",
            port = 443,
            uuidOrPassword = "98765432-10fe-dcba-9876-543210fedcba",
            alterId = 0,
            security = "auto",
            networkType = "ws",
            path = "/vmess-path",
            tls = "tls",
            sni = "nl-ams02.xraynode.net",
            pingMs = 88,
            isSelected = false,
            lastTestedAt = System.currentTimeMillis() - 240_000,
            rawUri = "vmess://eyJhZGQiOiJubC1hbXMwMi54cmF5bm9kZS5uZXQiLCJhaWQiOjAsImhvc3QiOiJubC1hbXMwMi54cmF5bm9kZS5uZXQiLCJpZCI6Ijk4NzY1NDMyLTEwZmUtZGNiYS05ODc2LTU0MzIxMGZlZGNiYSIsIm5ldCI6IndzIiwicGF0aCI6Ii92bWVzcy1wYXRoIiwicG9ydCI6NDQzLCJwcyI6IvCfh7Tkg7AgTmV0aGVybGFuZHMgLSBBbXN0ZXJkYW0gU3RyZWFtIiwic2N5IjoiYXV0byIsInNuaSI6Im5sLWFtczAyLnhyYXlub2RlLm5ldCIsInRscyI6InRscyIsInR5cGUiOiJub25lIiwidiI6IjIifQ=="
        ),
        ServerConfig(
            id = 4,
            subscriptionId = 1,
            name = "🇨🇭 Switzerland - Zurich Trojan",
            protocol = ProtocolType.TROJAN,
            address = "ch-zrh01.xraynode.net",
            port = 443,
            uuidOrPassword = "TrojanStrongPassword2026",
            networkType = "grpc",
            path = "TrojanGrpcService",
            tls = "tls",
            sni = "ch-zrh01.xraynode.net",
            pingMs = 110,
            isSelected = false,
            lastTestedAt = System.currentTimeMillis() - 300_000,
            rawUri = "trojan://TrojanStrongPassword2026@ch-zrh01.xraynode.net:443?type=grpc&serviceName=TrojanGrpcService&security=tls&sni=ch-zrh01.xraynode.net#%F0%9F%87%A8%F0%9F%87%AD%20Switzerland%20-%20Zurich%20Trojan"
        ),
        ServerConfig(
            id = 5,
            subscriptionId = null,
            name = "🇬🇧 UK - London Shadowsocks",
            protocol = ProtocolType.SHADOWSOCKS,
            address = "uk-lon03.xraynode.net",
            port = 8388,
            uuidOrPassword = "ShadowsocksSecret2026!",
            security = "aes-256-gcm",
            pingMs = 65,
            isSelected = false,
            lastTestedAt = System.currentTimeMillis() - 60_000,
            rawUri = "ss://YWVzLTI1Ni1nY206U2hhZG93c29ja3NTZWNyZXQyMDI2IUA=@uk-lon03.xraynode.net:8388#%F0%9F%87%AC%F0%9F%87%A7%20UK%20-%20London%20Shadowsocks"
        )
    )
}
