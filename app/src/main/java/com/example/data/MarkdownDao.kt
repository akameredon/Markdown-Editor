package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkdownDao {
    @Query("SELECT * FROM markdown_documents WHERE isDeleted = 0 AND deviceOwner = :deviceOwner ORDER BY updatedAt DESC")
    fun getAllActiveDocuments(deviceOwner: String): Flow<List<MarkdownDocument>>

    @Query("SELECT * FROM markdown_documents WHERE id = :id AND deviceOwner = :deviceOwner")
    suspend fun getDocumentById(id: String, deviceOwner: String): MarkdownDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MarkdownDocument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<MarkdownDocument>)

    @Query("UPDATE markdown_documents SET isDeleted = 1, syncStatus = 'LOCAL', updatedAt = :timestamp WHERE id = :id AND deviceOwner = :deviceOwner")
    suspend fun softDeleteDocument(id: String, deviceOwner: String, timestamp: Long)

    @Query("SELECT * FROM markdown_documents WHERE deviceOwner = :deviceOwner AND syncStatus != 'SYNCED'")
    suspend fun getLocallyModifiedDocuments(deviceOwner: String): List<MarkdownDocument>

    @Query("DELETE FROM markdown_documents WHERE id = :id AND deviceOwner = :deviceOwner")
    suspend fun permanentlyDeleteDocument(id: String, deviceOwner: String)

    @Query("SELECT * FROM markdown_documents WHERE deviceOwner = :deviceOwner")
    suspend fun getDocumentsByOwner(deviceOwner: String): List<MarkdownDocument>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: DocumentVersion)

    @Query("SELECT * FROM document_versions WHERE documentId = :documentId AND deviceOwner = :deviceOwner ORDER BY timestamp DESC")
    fun getVersionsForDocument(documentId: String, deviceOwner: String): Flow<List<DocumentVersion>>

    @Query("DELETE FROM document_versions WHERE documentId = :documentId AND deviceOwner = :deviceOwner")
    suspend fun deleteVersionsForDocument(documentId: String, deviceOwner: String)
}
