package com.olab.studyqb.ui.common

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders `**bold**` markdown spans (the only formatting the original app's LLM-sourced
 * `details`/Analyze responses use) as bold text; approximate rich-text parity is
 * explicitly acceptable per the port's scope, so nothing fancier is implemented here.
 */
fun parseMarkdownBold(input: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    var lastIndex = 0
    for (match in regex.findAll(input)) {
        append(input.substring(lastIndex, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        lastIndex = match.range.last + 1
    }
    append(input.substring(lastIndex))
}

@Composable
fun MarkdownBoldText(
    text: String,
    modifier: Modifier = Modifier,
    selectable: Boolean = false
) {
    val annotated = remember(text) { parseMarkdownBold(text) }
    if (selectable) {
        SelectionContainer(modifier = modifier) {
            Text(text = annotated)
        }
    } else {
        Text(text = annotated, modifier = modifier)
    }
}
