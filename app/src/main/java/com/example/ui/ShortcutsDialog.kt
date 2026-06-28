package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsDialog(
    shortcutBold: String,
    shortcutItalic: String,
    shortcutPreview: String,
    shortcutFocus: String,
    shortcutSync: String,
    onSaveShortcut: (action: String, combination: String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingAction by remember { mutableStateOf<String?>(null) }
    var inputVal by remember { mutableStateOf("") }

    val shortcutsList = remember(shortcutBold, shortcutItalic, shortcutPreview, shortcutFocus, shortcutSync) {
        listOf(
            "Bold" to shortcutBold,
            "Italic" to shortcutItalic,
            "Preview Toggle" to shortcutPreview,
            "Focus Toggle" to shortcutFocus,
            "Sync Now" to shortcutSync
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Keyboard,
                        contentDescription = "Shortcuts Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Keyboard Shortcuts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "If you have a physical keyboard connected, these shortcuts let you format or navigate instantly. Tap any action to customize its binding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                shortcutsList.forEach { (action, binding) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                editingAction = action
                                inputVal = binding
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Action shortcut trigger",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = binding,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit binding",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (editingAction != null) {
                    AlertDialog(
                        onDismissRequest = { editingAction = null },
                        title = { Text("Customize ${editingAction}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Enter a valid shortcut combination (e.g., Ctrl+B, Ctrl+Shift+P, Alt+Z):",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = inputVal,
                                    onValueChange = { inputVal = it },
                                    label = { Text("Key Combination") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    editingAction?.let { action ->
                                        if (inputVal.isNotBlank()) {
                                            onSaveShortcut(action, inputVal.trim())
                                        }
                                    }
                                    editingAction = null
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingAction = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
