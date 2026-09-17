package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ProtocolType
import com.example.data.parser.V2RayParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Xray VPN", appName)
  }

  @Test
  fun `test vless uri parsing`() {
    val vlessUri = "vless://7f8b9e10-3d2a-4a5b-8c1d-9e0f1a2b3c4d@de-fra01.xraynode.net:443?type=ws&security=tls&path=%2Fvless-ws&sni=de-fra01.xraynode.net#GermanyFast"
    val server = V2RayParser.parseUri(vlessUri)
    assertNotNull(server)
    assertEquals(ProtocolType.VLESS, server?.protocol)
    assertEquals("de-fra01.xraynode.net", server?.address)
    assertEquals(443, server?.port)
    assertEquals("7f8b9e10-3d2a-4a5b-8c1d-9e0f1a2b3c4d", server?.uuidOrPassword)
    assertEquals("ws", server?.networkType)
    assertEquals("tls", server?.tls)
  }
}
