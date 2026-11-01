package com.olab.studyqb.network

import com.olab.studyqb.data.network.NetworkErrors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorsTest {

    private fun httpError(code: Int): HttpException =
        HttpException(Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType())))

    /**
     * The case that motivated this mapper: Groq answers 403 for VPN and datacenter exit IPs
     * before it looks at the API key, so the message must point at the network, not credentials.
     */
    @Test
    fun `403 blames the network and names VPN`() {
        val message = NetworkErrors.userMessage(httpError(403))
        assertTrue(message.contains("VPN"))
        assertTrue(message.contains("AI service"))
    }

    @Test
    fun `401 blames credentials, not the network`() {
        val message = NetworkErrors.userMessage(httpError(401))
        assertTrue(message.contains("credentials"))
        assertTrue(!message.contains("VPN"))
    }

    @Test
    fun `429 reuses the shared rate-limit string`() {
        assertEquals(NetworkErrors.RATE_LIMITED, NetworkErrors.userMessage(httpError(429)))
    }

    @Test
    fun `5xx reports a temporary outage`() {
        listOf(500, 502, 503).forEach { code ->
            assertTrue(NetworkErrors.userMessage(httpError(code)).contains("temporarily unavailable"))
        }
    }

    /** The "unable to resolve host www.qbreader.org" case. */
    @Test
    fun `DNS and connect failures report no connection`() {
        listOf(
            UnknownHostException("www.qbreader.org"),
            ConnectException("failed to connect"),
            SocketTimeoutException("timeout")
        ).forEach { t ->
            assertTrue(NetworkErrors.userMessage(t).contains("No internet connection"))
        }
    }

    @Test
    fun `other IO failures report a generic network error`() {
        assertTrue(NetworkErrors.userMessage(IOException("unexpected end of stream")).contains("Network error"))
    }

    /** No raw exception text may reach the UI for an unrecognized failure. */
    @Test
    fun `unknown failures fall back without leaking exception text`() {
        val message = NetworkErrors.userMessage(IllegalStateException("kaboom internal detail"))
        assertEquals("Sorry, something went wrong.", message)
        assertTrue(!message.contains("kaboom"))
    }

    @Test
    fun `no mapped message contains raw HTTP status text`() {
        listOf(401, 403, 404, 429, 500).map { httpError(it) }.forEach { t ->
            val message = NetworkErrors.userMessage(t)
            assertTrue("leaked status in: $message", !message.contains("HTTP"))
        }
    }
}
