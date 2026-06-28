package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock()
    data class BulletItem(val text: AnnotatedString) : MarkdownBlock()
    data class BlockQuote(val text: AnnotatedString) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val blocks = parseMarkdown(markdown)

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (blocks.isEmpty()) {
            Text(
                text = "Start writing markdown to see the real-time preview...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic
            )
        } else {
            blocks.forEach { block ->
                RenderBlock(block)
            }
        }
    }
}

@Composable
fun RenderBlock(block: MarkdownBlock) {
    val uriHandler = LocalUriHandler.current

    when (block) {
        is MarkdownBlock.Header -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp)
                2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp)
                else -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            val bottomPadding = if (block.level == 1) 4.dp else 2.dp
            Column(modifier = Modifier.padding(top = 8.dp, bottom = bottomPadding)) {
                Text(
                    text = block.text,
                    style = style,
                    color = MaterialTheme.colorScheme.primary
                )
                if (block.level == 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 4.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }
        }
        is MarkdownBlock.Paragraph -> {
            ClickableText(
                text = block.text,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                onClick = { offset ->
                    block.text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                uriHandler.openUri(annotation.item)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                }
            )
        }
        is MarkdownBlock.CodeBlock -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    ),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = block.code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        is MarkdownBlock.BulletItem -> {
            Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                ClickableText(
                    text = block.text,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    onClick = { offset ->
                        block.text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (e: Exception) {
                                }
                            }
                    }
                )
            }
        }
        is MarkdownBlock.BlockQuote -> {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(IntrinsicSize.Min)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ClickableText(
                    text = block.text,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp
                    ),
                    onClick = { offset ->
                        block.text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (e: Exception) {
                                }
                            }
                    }
                )
            }
        }
        is MarkdownBlock.Divider -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
        }
    }
}

/**
 * Parsers markdown content into logical Block objects.
 */
fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var inCodeBlock = false
    val currentCodeLines = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(currentCodeLines.toString().trimEnd()))
                currentCodeLines.setLength(0)
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            currentCodeLines.append(line).append("\n")
            continue
        }

        val trimmed = line.trim()

        // Empty Line
        if (trimmed.isEmpty()) {
            continue
        }

        // Headers
        if (trimmed.startsWith("#")) {
            val match = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
            if (match != null) {
                val level = match.groupValues[1].length
                val title = match.groupValues[2]
                blocks.add(MarkdownBlock.Header(level, title))
                continue
            }
        }

        // Dividers
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            continue
        }

        // Blockquotes
        if (trimmed.startsWith(">")) {
            val content = trimmed.substring(1).trim()
            blocks.add(MarkdownBlock.BlockQuote(parseInlineFormatting(content)))
            continue
        }

        // Bullet Lists
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            val content = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(parseInlineFormatting(content)))
            continue
        }

        // Standard Paragraph
        blocks.add(MarkdownBlock.Paragraph(parseInlineFormatting(line)))
    }

    // Unclosed code blocks
    if (inCodeBlock && currentCodeLines.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(currentCodeLines.toString().trimEnd()))
    }

    return blocks
}

/**
 * Regex parser to apply bold, italic, inline code, and link styling to standard text lines.
 */
fun parseInlineFormatting(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentText = text

        // Regex definitions
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        val italicRegex = Regex("\\*(.*?)\\*")
        val inlineCodeRegex = Regex("`(.*?)`")
        val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")

        // For inline styling, we find ranges and apply formatting to our buildAnnotatedString
        // To make a clean inline parser in Kotlin Compose, we parse in priority: links, inline-code, bold, italic.
        // We can do a character scan or regex matching. Let's do a fast multi-pass regex compiler that is robust.
        
        // Find all links first, replace with placeholders or handle them sequentially
        // To keep the implementation simple and 100% stable, we can parse links, code, bold, and italic.
        // Let's implement a clean state scanner:
        var i = 0
        while (i < currentText.length) {
            val remaining = currentText.substring(i)

            // 1. Link parsing: [label](url)
            val linkMatch = linkRegex.find(remaining)
            if (linkMatch != null && remaining.startsWith(linkMatch.value)) {
                val label = linkMatch.groupValues[1]
                val url = linkMatch.groupValues[2]
                
                pushStringAnnotation(tag = "URL", annotation = url)
                pushStyle(
                    SpanStyle(
                        color = Color(0xFFD0BCFF), // Immersive lavender accent for links
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )
                )
                append(label)
                pop()
                pop()
                
                i += linkMatch.value.length
                continue
            }

            // 2. Bold/Italic combining: **bold**
            val boldMatch = boldRegex.find(remaining)
            if (boldMatch != null && remaining.startsWith(boldMatch.value)) {
                val content = boldMatch.groupValues[1]
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(content)
                pop()
                i += boldMatch.value.length
                continue
            }

            // 3. Italic: *italic*
            val italicMatch = italicRegex.find(remaining)
            if (italicMatch != null && remaining.startsWith(italicMatch.value)) {
                val content = italicMatch.groupValues[1]
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(content)
                pop()
                i += italicMatch.value.length
                continue
            }

            // 4. Inline code: `code`
            val codeMatch = inlineCodeRegex.find(remaining)
            if (codeMatch != null && remaining.startsWith(codeMatch.value)) {
                val content = codeMatch.groupValues[1]
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = Color(0xFF2D2F33), // Slate background
                        color = Color(0xFF00E5FF) // Immersive cyan highlight
                    )
                )
                append(" $content ")
                pop()
                i += codeMatch.value.length
                continue
            }

            // Default character
            append(currentText[i])
            i++
        }
    }
}
