package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.util.MarkdownExporter
import java.io.File
import java.io.FileOutputStream

@Composable
fun ExportDialog(
    documentTitle: String,
    documentContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("export_dialog"),
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
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export Document",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select a format to share or save.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Generating document format...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Export as HTML
                        ExportOptionRow(
                            title = "Web Page (HTML)",
                            description = "Clean markdown compiled into a single-file HTML document featuring dark/light adaptive stylesheets.",
                            icon = Icons.Default.Html,
                            tint = Color(0xFF00E5FF),
                            onClick = {
                                isExporting = true
                                handleExport(context, documentTitle, documentContent, "html") {
                                    isExporting = false
                                    onDismiss()
                                }
                            },
                            testTag = "export_html"
                        )

                        // 2. Export as PDF
                        ExportOptionRow(
                            title = "Printable Document (PDF)",
                            description = "High fidelity vector PDF. Word-wrapped text with header styling, page counts, and standard padding.",
                            icon = Icons.Default.PictureAsPdf,
                            tint = Color(0xFFF44336),
                            onClick = {
                                isExporting = true
                                handleExport(context, documentTitle, documentContent, "pdf") {
                                    isExporting = false
                                    onDismiss()
                                }
                            },
                            testTag = "export_pdf"
                        )

                        // 3. Export as DOCX
                        ExportOptionRow(
                            title = "Word Document (DOCX)",
                            description = "Packaged XML structure compatible with standard word processors such as Microsoft Word or Google Docs.",
                            icon = Icons.Default.Description,
                            tint = Color(0xFF2196F3),
                            onClick = {
                                isExporting = true
                                handleExport(context, documentTitle, documentContent, "docx") {
                                    isExporting = false
                                    onDismiss()
                                }
                            },
                            testTag = "export_docx"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isExporting) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportOptionRow(
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
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
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

private fun handleExport(
    context: Context,
    title: String,
    content: String,
    format: String,
    onComplete: () -> Unit
) {
    try {
        val file: File = when (format) {
            "html" -> {
                val htmlString = MarkdownExporter.exportToHtml(title, content)
                val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val cacheFile = File(context.cacheDir, "$cleanTitle.html")
                FileOutputStream(cacheFile).use { out ->
                    out.write(htmlString.toByteArray())
                }
                cacheFile
            }
            "pdf" -> {
                MarkdownExporter.exportToPdf(context, title, content)
            }
            "docx" -> {
                MarkdownExporter.exportToDocx(context, title, content)
            }
            else -> throw IllegalArgumentException("Unknown format: $format")
        }

        shareExportedFile(context, file, format)
        Toast.makeText(context, "Successfully exported to ${format.uppercase()}!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    } finally {
        onComplete()
    }
}

private fun shareExportedFile(context: Context, file: File, format: String) {
    val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
    
    val mimeType = when (format) {
        "html" -> "text/html"
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> "*/*"
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Exported Markdown: ${file.nameWithoutExtension}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share Exported Document"))
}
