package com.olab.studyqb.data.network.groq

import com.olab.studyqb.data.network.qbreader.dto.TossupDto
import com.olab.studyqb.domain.SubjectTypes

/**
 * The three Groq prompts, transcribed verbatim from the iOS source
 * (AnalyzeView.swift's `processFetchedData` and BuzzInView.swift's `buildTossupPrompt`) so
 * generated content matches the original app's behavior exactly.
 */
object GroqPrompts {

    /** Fixed model used for both Buzz In prompts (not part of the rotation), matching iOS's `tossupModel`. */
    const val TOSSUP_MODEL = "openai/gpt-oss-safeguard-20b"

    /** Verbatim from AnalyzeView.swift's `processFetchedData`. */
    fun clueAnalysisPrompt(answerQuery: String, questions: List<TossupDto>): String {
        val builder = StringBuilder(
            "Given these quizbowl questions, explain the most important clues worth remembering. " +
                "The clues should be specific. Clues that are frequent or involve significant names, " +
                "objects, or processes should be prioritized. Try to list between 10 and 15 clues. " +
                "The answer is $answerQuery. Within the prompt, the questions will be delineated by " +
                "the words END QUESTION. Order the clues so more common ones listed first. Use new " +
                "lines (\\n) to separate clues. Provide a description of each clue you found, in " +
                "enough depth for someone new to the topic to understand. Do not explain why you " +
                "chose the clues. Bold each clue. Do not bold the descriptions. Do not generate any " +
                "extraneous information. The questions are as follows: "
        )
        questions.forEachIndexed { i, question ->
            builder.append(
                "This is question number ${i + 1}. The answer is ${question.answerSanitized}. " +
                    "The question is as follows: ${question.questionSanitized} END QUESTION. "
            )
        }
        return builder.toString()
    }

    /** Verbatim from BuzzInView.swift's subject-classification prompt. */
    fun subjectClassificationPrompt(answer: String, categoryDisplayName: String): String = """
        You are classifying quizbowl answers.

        Given an answer, determine the most appropriate subject type for referring to that answer in a quizbowl tossup.

        Choose EXACTLY ONE subject type from the following list:

        ${SubjectTypes.promptListBlock}

        Guidelines:
        - Choose the most natural subject type for beginning every sentence of a tossup (e.g. "This novel...", "This composer...", "This battle...").
        - Prefer commonly used categories over overly specific ones
        - If multiple types could apply, choose the one that would sound most natural repeated throughout a quizbowl tossup
        - Do not invent new subject types, unless you are very confident that the subject is not described by any word in the list

        The answer is: $answer
        The category is: $categoryDisplayName
    """.trimIndent()

    /** Verbatim from BuzzInView.swift's tossup-stitching prompt. `reversedClues` = hardest-first clue lines. */
    fun tossupStitchingPrompt(categoryDisplayName: String, answer: String, subjectType: String, reversedClues: List<String>): String {
        val clueList = reversedClues.joinToString("\n") { "• $it" }
        return """
            You are stitching bullet-point clues into a quizbowl tossup paragraph. The subject category is $categoryDisplayName. The intended answer is $answer. The subject type is $subjectType. YOU MUST NOT MENTION THE ANSWER ANYWHERE IN YOUR RESPONSE!

            The clues are as follows (ordered hard to easy):
            $clueList

            Based on the clues, write a single flowing paragraph that:
            - Uses "This [subject type]" at the start of each sentence
            - Incorporates each clue as natural prose WITHOUT changing the meaning; If a clue is phrased about another entity (such as an author, composer, discoverer, character, or location), rewrite it so it becomes a fact about the answer while preserving its meaning.
            - Uses every clue exactly once, in order from hardest to easiest and does not add, remove, or alter factual information.
            - Does NOT state or hint at the answer
            - Produces approximately one sentence per clue without any additional details
            - Contains no bullet points, numbered lists, or markdown
            - The final sentence/clue should begin with the phrase "For 10 points, name this [answer_type] ... [rest of clue]"
            Output only the paragraph, nothing else.
        """.trimIndent()
    }

    /** Strips <think>...</think> reasoning blocks some models (e.g. Qwen) inline into their content. */
    fun stripReasoningTags(content: String): String =
        content.replace(Regex("(?s)<think>.*?</think>"), "").trim()
}
