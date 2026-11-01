package com.olab.studyqb.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.olab.studyqb.data.network.qbreader.QbReaderApi

/**
 * Hermetic tests against a real captured qbreader.org response shape (verified live against
 * the actual API while building the networking layer: /api/query wraps tossups in
 * {"count":N,"questionArray":[...]}, NOT a bare array).
 */
class QbReaderApiTest {
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
    fun query_parsesNestedTossupsWrapper() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"tossups":{"count":1,"questionArray":[
                    {"question_sanitized":"This figure led France.","answer_sanitized":"Napoleon Bonaparte","difficulty":4}
                ]}}
                """.trimIndent()
            )
        )

        val response = api.query(queryString = "Napoleon", difficulties = "1,2,3,4,5")

        assertEquals(1, response.tossups.count)
        assertEquals("Napoleon Bonaparte", response.tossups.questionArray[0].answerSanitized)
        assertEquals(4, response.tossups.questionArray[0].difficulty)
    }

    @Test
    fun frequencyList_parsesEntries() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"frequencyList":[{"answer":"Ottoman Empire","count":66},{"answer":"Charlemagne","count":53}]}"""
            )
        )

        val response = api.frequencyList(category = "History", difficulties = "1,2,3")

        assertEquals(2, response.frequencyList.size)
        assertEquals("Ottoman Empire", response.frequencyList[0].answer)
        assertEquals(66, response.frequencyList[0].count)
    }
}
