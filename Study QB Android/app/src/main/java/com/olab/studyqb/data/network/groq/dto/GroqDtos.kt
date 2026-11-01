package com.olab.studyqb.data.network.groq.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.7
)

@Serializable
data class ChatCompletionResponseDto(
    val choices: List<ChoiceDto> = emptyList()
)

@Serializable
data class ChoiceDto(
    val message: ChatMessageDto
)

@Serializable
data class GroqErrorEnvelopeDto(
    val error: GroqErrorDto? = null
)

@Serializable
data class GroqErrorDto(
    val message: String = "",
    val type: String = "",
    val code: String = ""
)
