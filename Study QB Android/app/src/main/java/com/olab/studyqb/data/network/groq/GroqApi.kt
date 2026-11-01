package com.olab.studyqb.data.network.groq

import com.olab.studyqb.data.network.groq.dto.ChatCompletionRequestDto
import com.olab.studyqb.data.network.groq.dto.ChatCompletionResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(@Body body: ChatCompletionRequestDto): ChatCompletionResponseDto

    companion object {
        const val BASE_URL = "https://api.groq.com/"
    }
}
