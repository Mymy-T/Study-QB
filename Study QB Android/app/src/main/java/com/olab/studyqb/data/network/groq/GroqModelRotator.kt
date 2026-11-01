package com.olab.studyqb.data.network.groq

import retrofit2.HttpException

/**
 * Round-robin model rotation matching AnalyzeView.swift's behavior: advance to the next
 * model ONLY on a rate-limit (HTTP 429) response, not on other errors.
 *
 * Mirrors AnalyzeView.swift:60 as of the 2026-10-18 iOS update, which cut the list from five
 * models to three alongside the cached-dataset work — dropping moonshotai/kimi-k2-instruct and
 * llama-3.1-8b-instant, and moving qwen3.6-27b to qwen3.8-27b.
 */
class GroqModelRotator {
    val models: List<String> = listOf(
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "qwen/qwen3.8-27b"
    )

    private var currentIndex = 0

    fun currentModel(): String = models[currentIndex]

    fun advance() {
        currentIndex = (currentIndex + 1) % models.size
    }

    companion object {
        fun isRateLimited(throwable: Throwable): Boolean =
            throwable is HttpException && throwable.code() == 429
    }
}
