package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DocumentTemplates

@Composable
fun TemplateSelectionDialog(
    onDismiss: () -> Unit,
    onSelectBlank: () -> Unit,
    onSelectTemplate: (DocumentTemplates.Template) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("template_dialog"),
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
                // Title Block
                Column {
                    Text(
                        text = "New Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a starter template to kickstart your thoughts, or begin with a blank canvas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // Options list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Blank Option Card
                    item {
                        TemplateItemCard(
                            title = "Blank Canvas",
                            description = "Start fresh with a clean slate. No templates, just pure distraction-free markdown.",
                            icon = Icons.Default.NoteAdd,
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                onSelectBlank()
                                onDismiss()
                            },
                            testTag = "template_blank"
                        )
                    }

                    // Pre-packaged Templates
                    items(DocumentTemplates.templates) { template ->
                        val (icon, tint) = when (template.iconName) {
                            "menu_book" -> Pair(Icons.Default.MenuBook, MaterialTheme.colorScheme.primary)
                            "edit" -> Pair(Icons.Default.EditNote, Color(0xFF00E5FF))
                            else -> Pair(Icons.Default.Groups, Color(0xFFD0BCFF))
                        }
                        
                        val desc = when (template.name) {
                            "Personal Journal" -> "Formatted with daily reflections, gratitude boards, mood trackers, and checkable goals."
                            "Blog Post Draft" -> "A structured structure including header specs, quote banners, lists, and Kotlin code examples."
                            else -> "Ideal for teams. Outlines agenda schedules, detailed note slots, and individual task items."
                        }

                        TemplateItemCard(
                            title = template.name,
                            description = desc,
                            icon = icon,
                            tint = tint,
                            onClick = {
                                onSelectTemplate(template)
                                onDismiss()
                            },
                            testTag = "template_${template.name.lowercase().replace(" ", "_")}"
                        )
                    }
                }

                // Dismiss Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
