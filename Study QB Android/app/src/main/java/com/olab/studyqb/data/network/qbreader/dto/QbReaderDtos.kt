package com.olab.studyqb.data.network.qbreader.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * qbreader.org's `/api/query` response shape, confirmed against the live API:
 * `{"tossups":{"count":N,"questionArray":[...]}}` (NOT a bare array under "tossups").
 */
@Serializable
data class QueryResponseDto(
    val tossups: TossupsWrapperDto = TossupsWrapperDto()
)

@Serializable
data class TossupsWrapperDto(
    val count: Int = 0,
    val questionArray: List<TossupDto> = emptyList()
)

@Serializable
data class TossupDto(
    @SerialName("question_sanitized") val questionSanitized: String,
    @SerialName("answer_sanitized") val answerSanitized: String,
    val difficulty: Int
)

@Serializable
data class FrequencyListResponseDto(
    val frequencyList: List<FrequencyEntryDto> = emptyList()
)

@Serializable
data class FrequencyEntryDto(
    val answer: String,
    val count: Int
)
