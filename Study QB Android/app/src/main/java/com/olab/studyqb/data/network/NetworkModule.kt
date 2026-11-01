package com.olab.studyqb.data.network

import com.olab.studyqb.BuildConfig
import com.olab.studyqb.data.network.groq.GroqApi
import com.olab.studyqb.data.network.qbreader.QbReaderApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val qbReaderClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val groqClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val qbReaderApi: QbReaderApi by lazy {
        Retrofit.Builder()
            .baseUrl(QbReaderApi.BASE_URL)
            .client(qbReaderClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(QbReaderApi::class.java)
    }

    val groqApi: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl(GroqApi.BASE_URL)
            .client(groqClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GroqApi::class.java)
    }

    val isGroqConfigured: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()
}
