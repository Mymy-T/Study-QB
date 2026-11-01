package com.olab.studyqb.data.local

import android.content.Context
import android.util.Log
import com.olab.studyqb.domain.Category
import com.olab.studyqb.domain.FuzzyMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * One pre-analyzed answer line from the bundled dataset. Mirrors DataModel.swift's `CachedItem`.
 *
 * `answer` is a filename-style slug ("madame_bovary"), so underscores become spaces before
 * matching. `details` is the finished markdown shown to the user — it is not regenerated.
 */
@Serializable
data class CachedClue(
    val answer: String,
    val category: String,
    val details: String,
    val model: String,
    val num_questions_analyzed: Int
) {
    val answerText: String get() = answer.replace('_', ' ')
}

/**
 * Looks up an Analyze query against `assets/dataset.jsonl` — 1010 answer lines across the ten
 * categories, pre-analyzed offline so common queries need no network call at all.
 *
 * Matches AnalyzeView.swift's `lookupCachedQuery`, which streams the bundled JSONL and returns
 * the first fuzzy answer match. One deliberate difference: iOS ignores the user's selected
 * categories on a cache hit, so a Literature entry comes back even with only Science selected.
 * We require the cached category to be among the selected ones, so the filter pills keep
 * meaning what they say. (Difficulty cannot be honored either way — the dataset records none.)
 */
class CachedClueDataSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Pulls just the `answer` value out of a raw line so the common case — no match — never
     * pays to decode the ~4 KB `details` body. Only a candidate line is fully parsed.
     */
    private val answerFieldRegex = Regex("\"answer\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")

    suspend fun lookup(query: String, selectedCategories: Set<Category>): CachedClue? =
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(ASSET_NAME).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        coroutineContext.ensureActive()
                        if (line.isBlank()) continue

                        val slug = answerFieldRegex.find(line)?.groupValues?.get(1) ?: continue
                        if (!FuzzyMatch.isMatch(query, slug.replace('_', ' '))) continue

                        val clue = try {
                            json.decodeFromString<CachedClue>(line)
                        } catch (t: Throwable) {
                            Log.w(TAG, "Skipping undecodable dataset line for '$slug'", t)
                            continue
                        }

                        val category = categoryOf(clue.category)
                        if (category == null) {
                            Log.w(TAG, "Unknown category '${clue.category}' for '$slug'")
                            continue
                        }
                        if (selectedCategories.isNotEmpty() && category !in selectedCategories) continue

                        return@useLines clue
                    }
                    null
                }
            } catch (e: IOException) {
                // A missing or unreadable asset is not fatal: the caller falls through to the
                // live qbreader + Groq path, exactly as iOS does when the bundle lookup fails.
                Log.w(TAG, "Cached dataset unavailable; falling back to live analysis", e)
                null
            }
        }

    private fun categoryOf(datasetKey: String): Category? =
        categoryByDatasetKey[datasetKey.lowercase()]

    private companion object {
        const val TAG = "CachedClueDataSource"
        const val ASSET_NAME = "dataset.jsonl"

        /** "Social Science" -> "social_science", matching the dataset's directory naming. */
        val categoryByDatasetKey: Map<String, Category> =
            Category.all.associateBy { it.displayName.lowercase().replace(' ', '_') }
    }
}
