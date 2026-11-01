package com.olab.studyqb.data.network.qbreader

import com.olab.studyqb.data.network.qbreader.dto.FrequencyListResponseDto
import com.olab.studyqb.data.network.qbreader.dto.QueryResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface QbReaderApi {

    @GET("api/query")
    suspend fun query(
        @Query("queryString") queryString: String,
        @Query("searchType") searchType: String = "answer",
        @Query("exactPhrase") exactPhrase: Boolean = true,
        @Query("difficulties") difficulties: String,
        @Query("categories") categories: String? = null
    ): QueryResponseDto

    @GET("api/frequency-list")
    suspend fun frequencyList(
        @Query("category") category: String,
        @Query("difficulties") difficulties: String,
        @Query("limit") limit: Int = 100
    ): FrequencyListResponseDto

    companion object {
        const val BASE_URL = "https://www.qbreader.org/"
    }
}
