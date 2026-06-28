package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_versions")
data class DocumentVersion(
    @PrimaryKey
    val versionId: String,       // UUID
    val documentId: String,      // Associated MarkdownDocument.id
    val deviceOwner: String,     // "DEVICE_A", "DEVICE_B", or "CLOUD"
    val title: String,
    val content: String,
    val timestamp: Long,
    val label: String            // "Auto-save", "Significant Edit", "Manual Snapshot", "Restored"
)
