package com.olab.studyqb.network

import com.olab.studyqb.data.network.groq.GroqApi
import com.olab.studyqb.data.network.groq.GroqModelRotator
import com.olab.studyqb.data.network.groq.dto.ChatCompletionRequestDto
import com.olab.studyqb.data.network.groq.dto.ChatMessageDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Verified against the real Groq API response shape (a live curl call) while building this layer. */
class GroqApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GroqApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GroqApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun chatCompletion_parsesMessageContent() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"role":"assistant","content":"pong"}}]}"""
            )
        )

        val response = api.chatCompletion(
            ChatCompletionRequestDto(model = "llama-3.3-70b-versatile", messages = listOf(ChatMessageDto("user", "ping")))
        )

        assertEquals("pong", response.choices.first().message.content)
    }

    @Test
    fun rateLimit429_isDetectedByModelRotator() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":{"message":"rate limited"}}"""))

        var caught: Throwable? = null
        try {
            api.chatCompletion(ChatCompletionRequestDto(model = "llama-3.3-70b-versatile", messages = listOf(ChatMessageDto("user", "ping"))))
        } catch (t: Throwable) {
            caught = t
        }

        assertTrue(caught is HttpException)
        assertTrue(GroqModelRotator.isRateLimited(caught!!))
    }

    @Test
    fun nonRateLimitError_isNotFlaggedAsRateLimited() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":{"message":"server error"}}"""))

        var caught: Throwable? = null
        try {
            api.chatCompletion(ChatCompletionRequestDto(model = "llama-3.3-70b-versatile", messages = listOf(ChatMessageDto("user", "ping"))))
        } catch (t: Throwable) {
            caught = t
        }

        assertTrue(caught != null)
        assertTrue(!GroqModelRotator.isRateLimited(caught!!))
    }
}
