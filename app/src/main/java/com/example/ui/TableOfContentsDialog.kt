package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class TocHeader(
    val level: Int,
    val text: String,
    val charIndex: Int
)

@Composable
fun TableOfContentsDialog(
    documentContent: String,
    onDismiss: () -> Unit,
    onHeaderClick: (Int) -> Unit
) {
    // Parse headers in real time
    val headers = remember(documentContent) {
        val parsed = mutableListOf<TocHeader>()
        var currentIndex = 0
        val lines = documentContent.split("\n")
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                val match = Regex("^(#+)\\s+(.+)$").find(trimmed)
                if (match != null) {
                    val level = match.groupValues[1].length
                    val text = match.groupValues[2]
                    // Find the precise start index in the documentContent
                    // Note: currentIndex represents the beginning of the line
                    parsed.add(TocHeader(level, text, currentIndex))
                }
            }
            // Add line length + 1 (for newline character)
            currentIndex += line.length + 1
        }
        parsed
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .testTag("toc_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Table of Contents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Quickly navigate across sections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // Content list
                if (headers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No headers found in this document.\nAdd lines starting with '#' to generate a table of contents.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(headers) { header ->
                            val indent = ((header.level - 1) * 16).dp
                            val (style, sizeLabel, textStyle, bgColor) = when (header.level) {
                                1 -> Quad(
                                    MaterialTheme.colorScheme.primary,
                                    "H1",
                                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                2 -> Quad(
                                    MaterialTheme.colorScheme.secondary,
                                    "H2",
                                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                                )
                                else -> Quad(
                                    MaterialTheme.colorScheme.tertiary,
                                    "H${header.level}",
                                    MaterialTheme.typography.bodyMedium,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .padding(start = indent)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onHeaderClick(header.charIndex)
                                        onDismiss()
                                    }
                                    .testTag("toc_item_${header.charIndex}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Level Indicator Tag
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = style
                                    )
                                }

                                Text(
                                    text = header.text,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
