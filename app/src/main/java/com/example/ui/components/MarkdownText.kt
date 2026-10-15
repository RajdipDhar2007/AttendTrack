package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rich Markdown renderer for Chat messages and AI Copilot responses.
 * Parses headers, bold, italics, inline code, code blocks, bullet points,
 * numbered lists, quotes, tables, and dividers into real styled Compose elements.
 */
@Composable
fun MarkdownFormattedText(
    text: String,
    modifier: Modifier = Modifier,
    baseTextColor: Color = MaterialTheme.colorScheme.onSurface,
    isUser: Boolean = false
) {
    if (isUser) {
        // Plain message for user bubbles
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = baseTextColor,
            modifier = modifier
        )
        return
    }

    val lines = text.split("\n")
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Handle fenced code blocks: ```...```
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Ending code block
                    CodeBlockCard(code = codeBlockBuilder.toString().trimEnd())
                    codeBlockBuilder.clear()
                    inCodeBlock = false
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    // Starting code block
                    inCodeBlock = true
                }
                i++
                continue
            }

            if (inCodeBlock) {
                codeBlockBuilder.append(line).append("\n")
                i++
                continue
            }

            // Empty line -> paragraph spacer
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                i++
                continue
            }

            // Check for Markdown Table:
            if (isTableRow(trimmed) && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                val headers = parseRowCells(trimmed)
                val rows = mutableListOf<List<String>>()
                var j = i + 2
                while (j < lines.size && isTableRow(lines[j].trim())) {
                    rows.add(parseRowCells(lines[j].trim()))
                    j++
                }
                MarkdownTableCard(
                    headers = headers,
                    rows = rows,
                    baseTextColor = baseTextColor
                )
                i = j
                continue
            }

            // Horizontal Rule: --- or *** or ___
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    color = baseTextColor.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                i++
                continue
            }

            // Headings
            when {
                trimmed.startsWith("### ") -> {
                    val content = trimmed.removePrefix("### ").trim()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = parseInlineMarkdown(content, baseTextColor),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = baseTextColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                trimmed.startsWith("## ") -> {
                    val content = trimmed.removePrefix("## ").trim()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parseInlineMarkdown(content, baseTextColor),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = baseTextColor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                trimmed.startsWith("# ") -> {
                    val content = trimmed.removePrefix("# ").trim()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = parseInlineMarkdown(content, baseTextColor),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = baseTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Blockquote: > ...
                trimmed.startsWith("> ") -> {
                    val content = trimmed.removePrefix("> ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(content, baseTextColor),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = baseTextColor.copy(alpha = 0.85f)
                        )
                    }
                }
                // Bullet points: • or - or *
                trimmed.startsWith("• ") || (trimmed.startsWith("- ") && !trimmed.startsWith("---")) || (trimmed.startsWith("* ") && !trimmed.startsWith("***")) -> {
                    val content = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(content, baseTextColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = baseTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Numbered list: e.g. "1. ", "2. ", "12. "
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val number = trimmed.substringBefore(".").trim()
                    val content = trimmed.substringAfter(".").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$number.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = parseInlineMarkdown(content, baseTextColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = baseTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Standard text paragraph
                else -> {
                    Text(
                        text = parseInlineMarkdown(line, baseTextColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = baseTextColor,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
            i++
        }

        // Flush any unclosed code block
        if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
            CodeBlockCard(code = codeBlockBuilder.toString().trimEnd())
        }
    }
}

@Composable
fun MarkdownTableCard(
    headers: List<String>,
    rows: List<List<String>>,
    baseTextColor: Color,
    modifier: Modifier = Modifier
) {
    if (headers.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.forEachIndexed { colIdx, header ->
                        val minW = when {
                            header.contains("Subject", ignoreCase = true) -> 135.dp
                            header.contains("Faculty", ignoreCase = true) || header.contains("Teacher", ignoreCase = true) || header.contains("TA", ignoreCase = true) -> 145.dp
                            header.contains("Time", ignoreCase = true) -> 130.dp
                            header.contains("Status", ignoreCase = true) -> 110.dp
                            else -> 95.dp
                        }
                        Box(
                            modifier = Modifier
                                .widthIn(min = minW, max = 220.dp)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = header.trim(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Data Rows
                rows.forEachIndexed { rowIdx, rowCells ->
                    val isEven = rowIdx % 2 == 0
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isEven) Color.Transparent
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.forEachIndexed { colIdx, header ->
                            val cellText = rowCells.getOrNull(colIdx)?.trim() ?: ""
                            val minW = when {
                                header.contains("Subject", ignoreCase = true) -> 135.dp
                                header.contains("Faculty", ignoreCase = true) || header.contains("Teacher", ignoreCase = true) || header.contains("TA", ignoreCase = true) -> 145.dp
                                header.contains("Time", ignoreCase = true) -> 130.dp
                                header.contains("Status", ignoreCase = true) -> 110.dp
                                else -> 95.dp
                            }
                            Box(
                                modifier = Modifier
                                    .widthIn(min = minW, max = 220.dp)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = parseInlineMarkdown(cellText, baseTextColor),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = baseTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isTableRow(line: String): Boolean {
    val t = line.trim()
    if (!t.contains("|")) return false
    val pipeCount = t.count { it == '|' }
    return pipeCount >= 1 && (t.startsWith("|") || pipeCount >= 2)
}

private fun isTableSeparator(line: String): Boolean {
    val t = line.trim()
    if (!t.contains("|")) return false
    val clean = t.removePrefix("|").removeSuffix("|")
    val parts = clean.split("|")
    return parts.isNotEmpty() && parts.all { part ->
        val p = part.trim()
        p.isNotEmpty() && p.all { it == '-' || it == ':' || it == ' ' } && p.contains("-")
    }
}

private fun parseRowCells(line: String): List<String> {
    val t = line.trim()
    val clean = if (t.startsWith("|")) t.substring(1) else t
    val cleanEnd = if (clean.endsWith("|")) clean.substring(0, clean.length - 1) else clean
    return cleanEnd.split("|").map { it.trim() }
}

@Composable
private fun CodeBlockCard(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E24))
            .border(0.5.dp, Color(0xFF3A3A4A), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            color = Color(0xFFE2E8F0)
        )
    }
}

/**
 * Parses inline Markdown tags:
 * - ***bold italic***
 * - **bold**
 * - *italic* or _italic_
 * - `inline code`
 */
fun parseInlineMarkdown(input: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = input.length

        while (cursor < length) {
            // Check for inline code: `code`
            if (input[cursor] == '`') {
                val closeIndex = input.indexOf('`', cursor + 1)
                if (closeIndex != -1) {
                    val codeContent = input.substring(cursor + 1, closeIndex)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            background = baseColor.copy(alpha = 0.12f)
                        )
                    )
                    append(" $codeContent ")
                    pop()
                    cursor = closeIndex + 1
                    continue
                }
            }

            // Check for bold-italic: ***text***
            if (input.startsWith("***", cursor)) {
                val closeIndex = input.indexOf("***", cursor + 3)
                if (closeIndex != -1) {
                    val content = input.substring(cursor + 3, closeIndex)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(parseSubInline(content))
                    pop()
                    cursor = closeIndex + 3
                    continue
                }
            }

            // Check for bold: **text**
            if (input.startsWith("**", cursor)) {
                val closeIndex = input.indexOf("**", cursor + 2)
                if (closeIndex != -1) {
                    val content = input.substring(cursor + 2, closeIndex)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(parseSubInline(content))
                    pop()
                    cursor = closeIndex + 2
                    continue
                }
            }

            // Check for italic: *text* (when not preceded or followed by *)
            if (input[cursor] == '*' && (cursor + 1 < length && input[cursor + 1] != '*')) {
                val closeIndex = input.indexOf('*', cursor + 1)
                if (closeIndex != -1 && (closeIndex + 1 == length || input[closeIndex + 1] != '*')) {
                    val content = input.substring(cursor + 1, closeIndex)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(parseSubInline(content))
                    pop()
                    cursor = closeIndex + 1
                    continue
                }
            }

            // Check for italic: _text_
            if (input[cursor] == '_' && (cursor + 1 < length && input[cursor + 1] != '_')) {
                val closeIndex = input.indexOf('_', cursor + 1)
                if (closeIndex != -1) {
                    val content = input.substring(cursor + 1, closeIndex)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(parseSubInline(content))
                    pop()
                    cursor = closeIndex + 1
                    continue
                }
            }

            // Regular character
            append(input[cursor])
            cursor++
        }
    }
}

private fun parseSubInline(text: String): AnnotatedString {
    // Basic recursion for nested formatting like `code` inside bold
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length
        while (cursor < length) {
            if (text[cursor] == '`') {
                val close = text.indexOf('`', cursor + 1)
                if (close != -1) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    append(text.substring(cursor + 1, close))
                    pop()
                    cursor = close + 1
                    continue
                }
            }
            append(text[cursor])
            cursor++
        }
    }
}
