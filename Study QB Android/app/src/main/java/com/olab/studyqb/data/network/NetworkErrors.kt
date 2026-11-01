package com.olab.studyqb.data.network

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps network failures to messages a user can act on.
 *
 * Without this, Retrofit's own text reaches the screen verbatim — "HTTP 403 Forbidden" or
 * "Unable to resolve host www.qbreader.org: No address associated with hostname" — neither of
 * which tells the reader what to do. Groq in particular answers 403 (not 401) for requests from
 * VPN and datacenter exit IPs, rejecting them before it ever inspects the API key, so a plain
 * 403 almost always means "you are on a VPN" rather than anything about credentials.
 *
 * Code inspection mirrors [com.olab.studyqb.data.network.groq.GroqModelRotator.isRateLimited];
 * kept free of Android dependencies so it stays unit-testable.
 */
object NetworkErrors {

    const val RATE_LIMITED = "Rate limited on every available model. Please try again shortly."

    fun userMessage(t: Throwable): String = when {
        t is HttpException -> when {
            t.code() == 401 -> "The AI service rejected this app's credentials. Please report this."
            t.code() == 403 -> "Couldn't reach the AI service. If you're using a VPN, turn it off and try again."
            t.code() == 429 -> RATE_LIMITED
            t.code() >= 500 -> "The AI service is temporarily unavailable. Please try again shortly."
            else -> "Sorry, something went wrong."
        }
        t is UnknownHostException || t is ConnectException || t is SocketTimeoutException ->
            "No internet connection. Please check your connection and try again."
        // Catch-all for the rest of OkHttp's transport failures (SSL, abrupt close, and so on).
        t is IOException -> "Network error. Please check your connection and try again."
        else -> "Sorry, something went wrong."
    }
}
