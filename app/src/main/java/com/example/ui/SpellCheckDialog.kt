package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.SpellChecker
import com.example.util.SpellingError

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellCheckDialog(
    documentContent: String,
    customDictionary: Set<String>,
    onDismiss: () -> Unit,
    onAddWordToDictionary: (String) -> Unit,
    onReplaceWord: (startIndex: Int, endIndex: Int, replacement: String) -> Unit
) {
    // Generate errors reactively
    val spellingErrors = remember(documentContent, customDictionary) {
        val words = SpellChecker.extractWords(documentContent)
        words.filter { !SpellChecker.isCorrect(it.raw, customDictionary) }
            .map { spellingWord ->
                // Context sentence: extract surrounding text
                val start = maxOf(0, spellingWord.startIndex - 25)
                val end = minOf(documentContent.length, spellingWord.endIndex + 25)
                val snippet = documentContent.substring(start, end).replace('\n', ' ')
                
                val prefix = if (start > 0) "..." else ""
                val suffix = if (end < documentContent.length) "..." else ""
                val contextSentence = "$prefix$snippet$suffix"

                val suggestions = SpellChecker.getSuggestions(spellingWord.raw, customDictionary)
                SpellingError(spellingWord, suggestions, contextSentence)
            }
    }

    var activeIndex by remember { mutableStateOf(0) }
    
    // Safety check: ensure activeIndex doesn't exceed bounds on updates
    val currentError = if (activeIndex < spellingErrors.size) spellingErrors[activeIndex] else null

    // Manual replacement text field input
    var manualReplacementText by remember(currentError) { mutableStateOf(currentError?.word?.raw ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("spellcheck_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spell Checker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (spellingErrors.isEmpty()) {
                                "All clear!"
                            } else {
                                "Found ${spellingErrors.size} potential spelling issue(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Spellcheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                if (spellingErrors.isEmpty() || currentError == null) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Spelling Errors Found!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your document content is spelled perfectly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Awesome")
                        }
                    }
                } else {
                    // Active correction workspace
                    val wordToCorrect = currentError.word.raw

                    // Navigation indicator
                    Text(
                        text = "Issue ${activeIndex + 1} of ${spellingErrors.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Word and Context Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Suspected spelling:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = wordToCorrect,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Read colors outside the remember block as they are Composable properties
                            val errorColor = MaterialTheme.colorScheme.error
                            val errorContainerColor = MaterialTheme.colorScheme.errorContainer

                            // Render context sentence highlighting the word
                            val annotatedContext = remember(currentError, errorColor, errorContainerColor) {
                                buildAnnotatedString {
                                    val sentence = currentError.contextSentence
                                    val regex = Regex("\\b$wordToCorrect\\b", RegexOption.IGNORE_CASE)
                                    val match = regex.find(sentence)
                                    if (match != null) {
                                        append(sentence.substring(0, match.range.first))
                                        withStyle(
                                            style = SpanStyle(
                                                color = errorColor,
                                                fontWeight = FontWeight.Bold,
                                                background = errorContainerColor
                                            )
                                        ) {
                                            append(match.value)
                                        }
                                        append(sentence.substring(match.range.last + 1))
                                    } else {
                                        append(sentence)
                                    }
                                }
                            }

                            Text(
                                text = annotatedContext,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Suggestions row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (currentError.suggestions.isEmpty()) {
                            Text(
                                text = "No suggestions available.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentError.suggestions.forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = {
                                            onReplaceWord(
                                                currentError.word.startIndex,
                                                currentError.word.endIndex,
                                                suggestion
                                            )
                                            // Step index
                                            if (activeIndex >= spellingErrors.size - 1) {
                                                // Reset or dismiss if finished
                                                activeIndex = 0
                                            }
                                        },
                                        label = { Text(suggestion, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }
                    }

                    // Manual Input correction area
                    OutlinedTextField(
                        value = manualReplacementText,
                        onValueChange = { manualReplacementText = it },
                        label = { Text("Correction") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spellcheck_manual_input"),
                        singleLine = true,
                        trailingIcon = {
                            if (manualReplacementText.isNotBlank() && manualReplacementText != wordToCorrect) {
                                TextButton(
                                    onClick = {
                                        onReplaceWord(
                                            currentError.word.startIndex,
                                            currentError.word.endIndex,
                                            manualReplacementText.trim()
                                        )
                                        if (activeIndex >= spellingErrors.size - 1) {
                                            activeIndex = 0
                                        }
                                    }
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    )

                    // Stepper / Control Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Add to Dictionary
                        OutlinedButton(
                            onClick = {
                                onAddWordToDictionary(wordToCorrect)
                                if (activeIndex >= spellingErrors.size - 1) {
                                    activeIndex = 0
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Learn Word", fontSize = 11.sp, maxLines = 1)
                        }

                        // Ignore/Skip
                        OutlinedButton(
                            onClick = {
                                if (activeIndex < spellingErrors.size - 1) {
                                    activeIndex++
                                } else {
                                    activeIndex = 0
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ignore", fontSize = 11.sp, maxLines = 1)
                        }

                        // Close Dialog
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
