package com.olab.studyqb.domain

/**
 * The exact quizbowl "subject type" list used by BuzzInView.swift's subject-classification
 * prompt (`buildTossupPrompt`), transcribed verbatim from the iOS source so a generated
 * tossup can open with natural phrasing like "This composer...".
 */
object SubjectTypes {
    val all: List<String> = listOf(
        "person", "author", "poet", "playwright", "philosopher", "psychologist", "scientist",
        "physicist", "chemist", "biologist", "mathematician", "astronomer", "economist",
        "composer", "musician", "artist", "architect", "politician", "monarch",
        "religious figure", "mythological figure",

        "novel", "short story", "play", "poem", "epic poem", "essay", "speech", "book",
        "literary work", "character",

        "painting", "sculpture", "building", "architectural style", "opera", "ballet",
        "symphony", "concerto", "song", "musical work",

        "battle", "war", "treaty", "rebellion", "revolution", "historical event", "empire",
        "dynasty", "civilization",

        "country", "city", "river", "mountain", "island", "sea", "ocean", "desert",
        "continent", "region",

        "planet", "moon", "star", "galaxy", "constellation", "chemical element", "compound",
        "particle", "organism", "species", "disease", "anatomical structure",

        "theory", "law", "principle", "equation", "phenomenon", "theorem", "proof",
        "algorithm", "function",

        "religion", "religious text", "school of thought", "concept", "organization",
        "company", "language", "film", "television series", "artifact", "document", "award",
        "festival"
    )

    /** The multi-line "choose EXACTLY ONE subject type from the following list" block. */
    val promptListBlock: String = buildString {
        val groups = listOf(
            all.subList(0, 21), all.subList(21, 31), all.subList(31, 41),
            all.subList(41, 50), all.subList(50, 60), all.subList(60, 72),
            all.subList(72, 81), all.subList(81, 94)
        )
        groups.forEachIndexed { index, group ->
            append(group.joinToString("\n"))
            if (index != groups.lastIndex) append("\n\n")
        }
    }

    /** Best-effort match of a model's free-text classification response to a known subject type. */
    fun match(response: String): String? {
        val normalized = response.trim().lowercase().trim('.', '"', '\'')
        return all.firstOrNull { it.equals(normalized, ignoreCase = true) }
            ?: all.firstOrNull { normalized.contains(it.lowercase()) }
    }
}
