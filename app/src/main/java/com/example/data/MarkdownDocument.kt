package com.example.data

import androidx.room.Entity

@Entity(tableName = "markdown_documents", primaryKeys = ["id", "deviceOwner"])
data class MarkdownDocument(
    val id: String,          // Document UUID
    val deviceOwner: String,  // "DEVICE_A", "DEVICE_B", or "CLOUD"
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Int = 1,
    val isDeleted: Boolean = false,
    val syncStatus: String = "LOCAL" // "LOCAL", "SYNCED", "CONFLICT", "PENDING"
) {
    fun isConflict(): Boolean = syncStatus == "CONFLICT"
    fun isSynced(): Boolean = syncStatus == "SYNCED"
}
