package com.olab.studyqb.network

import com.olab.studyqb.data.network.qbreader.QbReaderApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Regression test for a real bug caught during review: manually URL-encoding the query
 * string before handing it to Retrofit's @Query param caused double-encoding (spaces became
 * literal "%2520" instead of "%20"), silently breaking every qbreader search. Retrofit/OkHttp
 * already percent-encodes non-`encoded` @Query params, so the raw string must be passed as-is.
 */
class QbReaderQueryEncodingTest {
    private lateinit var server: MockWebServer
    private lateinit var api: QbReaderApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(QbReaderApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun query_withSpaces_isEncodedExactlyOnce() = runTest {
        server.enqueue(MockResponse().setBody("""{"tossups":{"count":0,"questionArray":[]}}"""))

        api.query(queryString = "Napoleon Bonaparte", difficulties = "1,2,3")

        val requestedPath = server.takeRequest().path.orEmpty()
        assertTrue("expected a single %20 for the space, got: $requestedPath", requestedPath.contains("Napoleon%20Bonaparte"))
        assertFalse("query string was double-encoded: $requestedPath", requestedPath.contains("%2520"))
        assertFalse("query string used '+' instead of %20: $requestedPath", requestedPath.contains("Napoleon+Bonaparte"))
    }
}
