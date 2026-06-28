package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DocumentRepository(private val markdownDao: MarkdownDao) {

    private val httpClient = OkHttpClient()

    // Active documents for each simulated device
    val activeDocumentsDeviceA: Flow<List<MarkdownDocument>> = markdownDao.getAllActiveDocuments("DEVICE_A")
    val activeDocumentsDeviceB: Flow<List<MarkdownDocument>> = markdownDao.getAllActiveDocuments("DEVICE_B")

    suspend fun getDocumentById(id: String, deviceOwner: String): MarkdownDocument? {
        return markdownDao.getDocumentById(id, deviceOwner)
    }

    suspend fun insertDocument(document: MarkdownDocument) {
        markdownDao.insertDocument(document)
    }

    suspend fun softDeleteDocument(id: String, deviceOwner: String) {
        markdownDao.softDeleteDocument(id, deviceOwner, System.currentTimeMillis())
    }

    suspend fun permanentlyDeleteDocument(id: String, deviceOwner: String) {
        markdownDao.permanentlyDeleteDocument(id, deviceOwner)
        markdownDao.deleteVersionsForDocument(id, deviceOwner)
    }

    fun getVersionsForDocument(documentId: String, deviceOwner: String): Flow<List<DocumentVersion>> {
        return markdownDao.getVersionsForDocument(documentId, deviceOwner)
    }

    suspend fun insertVersion(version: DocumentVersion) {
        markdownDao.insertVersion(version)
    }

    suspend fun createNewDocument(title: String, content: String, deviceOwner: String): MarkdownDocument {
        val doc = MarkdownDocument(
            id = UUID.randomUUID().toString(),
            deviceOwner = deviceOwner,
            title = title,
            content = content,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1,
            isDeleted = false,
            syncStatus = "LOCAL"
        )
        markdownDao.insertDocument(doc)
        return doc
    }

    /**
     * Sync Engine.
     * Performs cloud sync for a given device.
     * If [serverUrl] is provided, it tries to sync with a real server.
     * Otherwise, it runs the Simulation Sync using the local 'CLOUD' partition.
     */
    suspend fun performSync(
        deviceOwner: String,
        serverUrl: String,
        apiToken: String
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            if (serverUrl.isNotBlank()) {
                // Real Cloud Sync
                return@withContext performRealCloudSync(deviceOwner, serverUrl, apiToken)
            } else {
                // Simulated Cloud Sync (perfect for local playground)
                return@withContext performSimulatedCloudSync(deviceOwner)
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Sync failed", e)
            return@withContext SyncResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Resolves a conflict.
     * [resolution] can be "KEEP_LOCAL", "KEEP_REMOTE", or "MERGED"
     */
    suspend fun resolveConflict(
        id: String,
        deviceOwner: String,
        resolution: String,
        mergedContent: String = ""
    ) = withContext(Dispatchers.IO) {
        val localDoc = markdownDao.getDocumentById(id, deviceOwner) ?: return@withContext
        val cloudDoc = markdownDao.getDocumentById(id, "CLOUD") ?: return@withContext

        val updatedLocal = when (resolution) {
            "KEEP_LOCAL" -> {
                // Keep local changes, force cloud to update
                val newVersion = maxOf(localDoc.version, cloudDoc.version) + 1
                val resolved = localDoc.copy(
                    version = newVersion,
                    syncStatus = "SYNCED",
                    updatedAt = System.currentTimeMillis()
                )
                markdownDao.insertDocument(resolved)
                markdownDao.insertDocument(resolved.copy(deviceOwner = "CLOUD"))
                resolved
            }
            "KEEP_REMOTE" -> {
                // Replace local with cloud
                val resolved = cloudDoc.copy(
                    deviceOwner = deviceOwner,
                    syncStatus = "SYNCED"
                )
                markdownDao.insertDocument(resolved)
                resolved
            }
            "MERGED" -> {
                // Merge content
                val newVersion = maxOf(localDoc.version, cloudDoc.version) + 1
                val resolved = localDoc.copy(
                    content = mergedContent,
                    version = newVersion,
                    syncStatus = "SYNCED",
                    updatedAt = System.currentTimeMillis()
                )
                markdownDao.insertDocument(resolved)
                markdownDao.insertDocument(resolved.copy(deviceOwner = "CLOUD"))
                resolved
            }
            else -> localDoc
        }
        Log.d("SyncEngine", "Resolved conflict for $id using $resolution")
    }

    /**
     * Simulated Cloud Sync:
     * Synchronizes [deviceOwner] ("DEVICE_A" or "DEVICE_B") with "CLOUD".
     */
    private suspend fun performSimulatedCloudSync(deviceOwner: String): SyncResult {
        val localDocs = markdownDao.getDocumentsByOwner(deviceOwner)
        val cloudDocs = markdownDao.getDocumentsByOwner("CLOUD").associateBy { it.id }

        val localModified = localDocs.filter { it.syncStatus != "SYNCED" }
        val localModifiedMap = localModified.associateBy { it.id }

        var uploadsCount = 0
        var downloadsCount = 0
        var conflictsCount = 0
        var deletedCount = 0

        val processedIds = mutableSetOf<String>()

        // 1. Process local modifications
        for (localDoc in localModified) {
            processedIds.add(localDoc.id)
            val cloudDoc = cloudDocs[localDoc.id]

            if (cloudDoc == null) {
                // Document only exists locally (created offline)
                if (localDoc.isDeleted) {
                    // Deleted locally before ever synced, just purge it
                    markdownDao.permanentlyDeleteDocument(localDoc.id, deviceOwner)
                    deletedCount++
                } else {
                    // Upload new document to cloud
                    val syncedDoc = localDoc.copy(syncStatus = "SYNCED")
                    markdownDao.insertDocument(syncedDoc)
                    markdownDao.insertDocument(syncedDoc.copy(deviceOwner = "CLOUD"))
                    uploadsCount++
                }
            } else {
                // Document exists on both
                if (localDoc.isDeleted) {
                    // Soft deleted locally, delete on cloud too
                    markdownDao.insertDocument(cloudDoc.copy(isDeleted = true, updatedAt = System.currentTimeMillis(), syncStatus = "SYNCED"))
                    markdownDao.permanentlyDeleteDocument(localDoc.id, deviceOwner)
                    deletedCount++
                } else if (cloudDoc.isDeleted) {
                    // Deleted on cloud but edited locally? That is a conflict!
                    markdownDao.insertDocument(localDoc.copy(syncStatus = "CONFLICT"))
                    conflictsCount++
                } else if (localDoc.version > cloudDoc.version) {
                    // Local is newer, upload to cloud
                    val syncedDoc = localDoc.copy(syncStatus = "SYNCED")
                    markdownDao.insertDocument(syncedDoc)
                    markdownDao.insertDocument(syncedDoc.copy(deviceOwner = "CLOUD"))
                    uploadsCount++
                } else if (cloudDoc.version > localDoc.version) {
                    // Cloud is newer, but we had local changes? Conflict!
                    if (localDoc.content != cloudDoc.content || localDoc.title != cloudDoc.title) {
                        markdownDao.insertDocument(localDoc.copy(syncStatus = "CONFLICT"))
                        conflictsCount++
                    } else {
                        // Contents are identical, just update local version & status
                        markdownDao.insertDocument(cloudDoc.copy(deviceOwner = deviceOwner, syncStatus = "SYNCED"))
                        downloadsCount++
                    }
                } else {
                    // Same versions, but different content? Conflict!
                    if (localDoc.content != cloudDoc.content || localDoc.title != cloudDoc.title) {
                        markdownDao.insertDocument(localDoc.copy(syncStatus = "CONFLICT"))
                        conflictsCount++
                    } else {
                        // Same versions and identical content. Sync status updated.
                        markdownDao.insertDocument(localDoc.copy(syncStatus = "SYNCED"))
                    }
                }
            }
        }

        // 2. Process items in cloud that device hasn't modified
        for ((id, cloudDoc) in cloudDocs) {
            if (id in processedIds) continue

            val localDoc = localDocs.firstOrNull { it.id == id }

            if (localDoc == null) {
                // Cloud document is missing locally. Download it!
                if (!cloudDoc.isDeleted) {
                    markdownDao.insertDocument(cloudDoc.copy(deviceOwner = deviceOwner, syncStatus = "SYNCED"))
                    downloadsCount++
                }
            } else {
                // Local exists but was SYNCED. Update it if cloud is newer.
                if (cloudDoc.isDeleted) {
                    // Deleted on cloud, delete locally
                    markdownDao.permanentlyDeleteDocument(id, deviceOwner)
                    deletedCount++
                } else if (cloudDoc.version > localDoc.version || cloudDoc.updatedAt > localDoc.updatedAt) {
                    markdownDao.insertDocument(cloudDoc.copy(deviceOwner = deviceOwner, syncStatus = "SYNCED"))
                    downloadsCount++
                } else {
                    // Up to date
                    if (localDoc.syncStatus != "SYNCED") {
                        markdownDao.insertDocument(localDoc.copy(syncStatus = "SYNCED"))
                    }
                }
            }
        }

        return SyncResult.Success(
            uploads = uploadsCount,
            downloads = downloadsCount,
            conflicts = conflictsCount,
            deleted = deletedCount,
            isSimulated = true
        )
    }

    /**
     * Real REST Sync Engine:
     * Connects to [serverUrl] using standard OkHttp to exchange JSON data.
     */
    private suspend fun performRealCloudSync(
        deviceOwner: String,
        serverUrl: String,
        apiToken: String
    ): SyncResult {
        val localModified = markdownDao.getLocallyModifiedDocuments(deviceOwner)
        
        // Prepare request body
        val jsonRequest = JSONObject().apply {
            put("token", apiToken)
            put("device", deviceOwner)
            val changesArray = JSONArray()
            for (doc in localModified) {
                changesArray.put(JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.title)
                    put("content", doc.content)
                    put("version", doc.version)
                    put("updatedAt", doc.updatedAt)
                    put("isDeleted", doc.isDeleted)
                })
            }
            put("changes", changesArray)
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiToken")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return SyncResult.Error("HTTP Error: ${response.code} - ${response.message}")
            }

            val bodyString = response.body?.string() ?: return SyncResult.Error("Empty server response")
            val jsonResponse = JSONObject(bodyString)
            
            // Server response structure should include remote changes to merge or conflict notifications
            // Standard synchronization resolution logic runs here
            val remoteChanges = jsonResponse.optJSONArray("changes")
            var uploadsCount = localModified.size
            var downloadsCount = 0
            var conflictsCount = 0
            var deletedCount = 0

            if (remoteChanges != null) {
                for (i in 0 until remoteChanges.length()) {
                    val changeObj = remoteChanges.getJSONObject(i)
                    val id = changeObj.getString("id")
                    val remoteTitle = changeObj.getString("title")
                    val remoteContent = changeObj.getString("content")
                    val remoteVersion = changeObj.getInt("version")
                    val remoteUpdatedAt = changeObj.getLong("updatedAt")
                    val remoteIsDeleted = changeObj.getBoolean("isDeleted")
                    val hasConflict = changeObj.optBoolean("conflict", false)

                    val localDoc = markdownDao.getDocumentById(id, deviceOwner)

                    if (hasConflict) {
                        // Mark as conflict locally and insert cloud version to compare
                        if (localDoc != null) {
                            markdownDao.insertDocument(localDoc.copy(syncStatus = "CONFLICT"))
                            
                            // Save cloud copy so the user can compare
                            markdownDao.insertDocument(MarkdownDocument(
                                id = id,
                                deviceOwner = "CLOUD",
                                title = remoteTitle,
                                content = remoteContent,
                                createdAt = remoteUpdatedAt,
                                updatedAt = remoteUpdatedAt,
                                version = remoteVersion,
                                isDeleted = remoteIsDeleted,
                                syncStatus = "SYNCED"
                            ))
                            conflictsCount++
                        }
                    } else {
                        if (remoteIsDeleted) {
                            markdownDao.permanentlyDeleteDocument(id, deviceOwner)
                            deletedCount++
                        } else {
                            markdownDao.insertDocument(MarkdownDocument(
                                id = id,
                                deviceOwner = deviceOwner,
                                title = remoteTitle,
                                content = remoteContent,
                                createdAt = remoteUpdatedAt,
                                updatedAt = remoteUpdatedAt,
                                version = remoteVersion,
                                isDeleted = false,
                                syncStatus = "SYNCED"
                            ))
                            downloadsCount++
                        }
                    }
                }
            }

            // Mark our successfully uploaded documents as SYNCED
            for (doc in localModified) {
                if (doc.syncStatus != "CONFLICT") {
                    if (doc.isDeleted) {
                        markdownDao.permanentlyDeleteDocument(doc.id, deviceOwner)
                    } else {
                        markdownDao.insertDocument(doc.copy(syncStatus = "SYNCED"))
                    }
                }
            }

            return SyncResult.Success(
                uploads = uploadsCount,
                downloads = downloadsCount,
                conflicts = conflictsCount,
                deleted = deletedCount,
                isSimulated = false
            )
        }
    }
}

sealed class SyncResult {
    data class Success(
        val uploads: Int,
        val downloads: Int,
        val conflicts: Int,
        val deleted: Int,
        val isSimulated: Boolean
    ) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
