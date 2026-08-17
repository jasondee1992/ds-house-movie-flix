package com.jasond.homeflix.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerAddressTest {
    @Test fun `plain emulator host receives scheme port and trailing slash`() {
        assertEquals("http://10.0.2.2:8000/", normalizeServerAddress("10.0.2.2"))
    }

    @Test fun `explicit LAN port is preserved`() {
        assertEquals("http://192.168.1.25:9000/", normalizeServerAddress("192.168.1.25:9000"))
    }

    @Test fun `full URL is normalized for Retrofit`() {
        assertEquals("https://homeflix.local:8000/", normalizeServerAddress("https://homeflix.local/"))
    }

    @Test fun `invalid or blank address is rejected`() {
        assertNull(normalizeServerAddress(""))
        assertNull(normalizeServerAddress("not a valid host name"))
    }
}
