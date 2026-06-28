package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MarkdownViewModel(
    application: Application,
    private val repository: DocumentRepository,
    private val settingsManager: SettingsManager
) : AndroidViewModel(application) {

    // Active simulated device workspace: "DEVICE_A" or "DEVICE_B"
    private val _currentDevice = MutableStateFlow("DEVICE_A")
    val currentDevice: StateFlow<String> = _currentDevice.asStateFlow()

    // Active document selection IDs for Device A and B
    private val _selectedDocIdA = MutableStateFlow<String?>(null)
    val selectedDocIdA: StateFlow<String?> = _selectedDocIdA.asStateFlow()

    private val _selectedDocIdB = MutableStateFlow<String?>(null)
    val selectedDocIdB: StateFlow<String?> = _selectedDocIdB.asStateFlow()

    // Observe active documents reactively from Room
    val documentsA: StateFlow<List<MarkdownDocument>> = repository.activeDocumentsDeviceA
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentsB: StateFlow<List<MarkdownDocument>> = repository.activeDocumentsDeviceB
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active document based on current workspace
    val activeDocuments: StateFlow<List<MarkdownDocument>> = combine(
        currentDevice, documentsA, documentsB
    ) { device, docsA, docsB ->
        if (device == "DEVICE_A") docsA else docsB
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDocId: StateFlow<String?> = combine(
        currentDevice, _selectedDocIdA, _selectedDocIdB
    ) { device, idA, idB ->
        if (device == "DEVICE_A") idA else idB
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeDocument = MutableStateFlow<MarkdownDocument?>(null)
    val activeDocument: StateFlow<MarkdownDocument?> = _activeDocument.asStateFlow()

    private val _activeVersions = MutableStateFlow<List<DocumentVersion>>(emptyList())
    val activeVersions: StateFlow<List<DocumentVersion>> = _activeVersions.asStateFlow()

    // Settings flows
    val themeMode: StateFlow<String> = settingsManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    val focusMode: StateFlow<Boolean> = settingsManager.focusModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncServerUrl: StateFlow<String> = settingsManager.syncServerUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val syncApiToken: StateFlow<String> = settingsManager.syncApiTokenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val lastSyncTime: StateFlow<Long> = settingsManager.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val simulatedDeviceBEnabled: StateFlow<Boolean> = settingsManager.simulatedDeviceBEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSaveEnabled: StateFlow<Boolean> = settingsManager.autoSaveEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customDictionary: StateFlow<Set<String>> = settingsManager.customDictionaryFlow
        .map { csv -> csv.split(",").filter { it.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Shortcut Settings
    val shortcutBold: StateFlow<String> = settingsManager.shortcutBoldFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ctrl+B")

    val shortcutItalic: StateFlow<String> = settingsManager.shortcutItalicFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ctrl+I")

    val shortcutPreview: StateFlow<String> = settingsManager.shortcutPreviewFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ctrl+P")

    val shortcutFocus: StateFlow<String> = settingsManager.shortcutFocusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ctrl+F")

    val shortcutSync: StateFlow<String> = settingsManager.shortcutSyncFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ctrl+S")

    // Syncing UI State
    private val _syncState = MutableStateFlow<SyncUIState>(SyncUIState.Idle)
    val syncState: StateFlow<SyncUIState> = _syncState.asStateFlow()

    // Selected View Mode: "EDIT", "PREVIEW", "SPLIT"
    private val _viewMode = MutableStateFlow("SPLIT")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    init {
        // Automatically monitor document selection changes and load its history
        var versionsJob: kotlinx.coroutines.Job? = null
        viewModelScope.launch {
            combine(selectedDocId, currentDevice) { id, device -> id to device }
                .collect { (id, device) ->
                    versionsJob?.cancel()
                    if (id != null) {
                        val doc = repository.getDocumentById(id, device)
                        _activeDocument.value = doc
                        versionsJob = viewModelScope.launch {
                            repository.getVersionsForDocument(id, device).collect { list ->
                                _activeVersions.value = list
                            }
                        }
                    } else {
                        _activeDocument.value = null
                        _activeVersions.value = emptyList()
                    }
                }
        }

        // Initialize selections when document list is loaded
        viewModelScope.launch {
            documentsA.collect { docs ->
                if (_selectedDocIdA.value == null && docs.isNotEmpty()) {
                    _selectedDocIdA.value = docs.first().id
                }
            }
        }
        viewModelScope.launch {
            documentsB.collect { docs ->
                if (_selectedDocIdB.value == null && docs.isNotEmpty()) {
                    _selectedDocIdB.value = docs.first().id
                }
            }
        }
    }

    fun setDevice(device: String) {
        _currentDevice.value = device
    }

    suspend fun getDocumentById(id: String, device: String): MarkdownDocument? {
        return repository.getDocumentById(id, device)
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
    }

    fun selectDocument(id: String) {
        if (_currentDevice.value == "DEVICE_A") {
            _selectedDocIdA.value = id
        } else {
            _selectedDocIdB.value = id
        }
    }

    fun updateDocumentContent(title: String, content: String) {
        val currentDoc = _activeDocument.value ?: return
        val updated = currentDoc.copy(
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "LOCAL" // Marked as modified locally
        )
        _activeDocument.value = updated
        
        // Only write to the DB automatically if auto-save is enabled
        if (autoSaveEnabled.value) {
            viewModelScope.launch {
                repository.insertDocument(updated)

                // Auto-save version logic on significant edit or elapsed time
                val lastVer = _activeVersions.value.firstOrNull()
                val timeDiff = if (lastVer != null) System.currentTimeMillis() - lastVer.timestamp else Long.MAX_VALUE
                val charDiff = if (lastVer != null) Math.abs(content.length - lastVer.content.length) else Int.MAX_VALUE

                // Auto-save if:
                // 1. No versions exist yet
                // 2. Character content length changed significantly (>= 40 characters)
                // 3. Title changed
                // 4. More than 2 minutes have elapsed since last auto-save version
                if (lastVer == null || charDiff >= 40 || title != lastVer.title || timeDiff > 120_000) {
                    val label = if (lastVer == null) "Initial Version" else "Auto-save (Significant Edit)"
                    val docVersion = DocumentVersion(
                        versionId = java.util.UUID.randomUUID().toString(),
                        documentId = currentDoc.id,
                        deviceOwner = currentDoc.deviceOwner,
                        title = title,
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        label = label
                    )
                    repository.insertVersion(docVersion)
                }
            }
        }
    }

    fun saveActiveDocumentManual() {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            repository.insertDocument(doc)
            
            // Explicit snapshot version when user triggers manual save
            val docVersion = DocumentVersion(
                versionId = java.util.UUID.randomUUID().toString(),
                documentId = doc.id,
                deviceOwner = doc.deviceOwner,
                title = doc.title,
                content = doc.content,
                timestamp = System.currentTimeMillis(),
                label = "Manual Save Checkpoint"
            )
            repository.insertVersion(docVersion)
        }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoSaveEnabled(enabled)
            // If they are enabling auto-save, save current active document state immediately
            if (enabled) {
                _activeDocument.value?.let { repository.insertDocument(it) }
            }
        }
    }

    fun addWordToDictionary(word: String) {
        viewModelScope.launch {
            val current = customDictionary.value.toMutableSet()
            if (current.add(word.trim().lowercase())) {
                settingsManager.setCustomDictionary(current)
            }
        }
    }

    fun removeWordFromDictionary(word: String) {
        viewModelScope.launch {
            val current = customDictionary.value.toMutableSet()
            if (current.remove(word.trim().lowercase())) {
                settingsManager.setCustomDictionary(current)
            }
        }
    }

    fun createVersionSnapshot(label: String) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            val docVersion = DocumentVersion(
                versionId = java.util.UUID.randomUUID().toString(),
                documentId = doc.id,
                deviceOwner = doc.deviceOwner,
                title = doc.title,
                content = doc.content,
                timestamp = System.currentTimeMillis(),
                label = label
            )
            repository.insertVersion(docVersion)
        }
    }

    fun restoreVersion(version: DocumentVersion) {
        val currentDoc = _activeDocument.value ?: return
        // Save current state before restoring so users never lose work accidentally
        createVersionSnapshot("Before restoring '${version.label}'")

        val updated = currentDoc.copy(
            title = version.title,
            content = version.content,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "LOCAL"
        )
        _activeDocument.value = updated
        viewModelScope.launch {
            repository.insertDocument(updated)
            // Save version representing this restoration event
            val docVersion = DocumentVersion(
                versionId = java.util.UUID.randomUUID().toString(),
                documentId = updated.id,
                deviceOwner = updated.deviceOwner,
                title = updated.title,
                content = updated.content,
                timestamp = System.currentTimeMillis(),
                label = "Restored: ${version.label}"
            )
            repository.insertVersion(docVersion)
        }
    }

    fun createNewDocument(title: String = "Untitled Note", content: String = "") {
        viewModelScope.launch {
            val device = _currentDevice.value
            val doc = repository.createNewDocument(title, content, device)
            if (device == "DEVICE_A") {
                _selectedDocIdA.value = doc.id
            } else {
                _selectedDocIdB.value = doc.id
            }
            // Auto-create initial version for the new document
            val initialVersion = DocumentVersion(
                versionId = java.util.UUID.randomUUID().toString(),
                documentId = doc.id,
                deviceOwner = doc.deviceOwner,
                title = doc.title,
                content = doc.content,
                timestamp = System.currentTimeMillis(),
                label = "Initial Note Created"
            )
            repository.insertVersion(initialVersion)
        }
    }

    fun createNewDocumentFromTemplate(template: DocumentTemplates.Template) {
        createNewDocument(title = template.title, content = template.content)
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            val device = _currentDevice.value
            repository.softDeleteDocument(id, device)
            
            // Re-select another document if available
            val remaining = if (device == "DEVICE_A") documentsA.value else documentsB.value
            val nextDoc = remaining.firstOrNull { it.id != id && !it.isDeleted }
            if (device == "DEVICE_A") {
                _selectedDocIdA.value = nextDoc?.id
            } else {
                _selectedDocIdB.value = nextDoc?.id
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncState.value = SyncUIState.Syncing
            val device = _currentDevice.value
            val url = syncServerUrl.value
            val token = syncApiToken.value

            val result = repository.performSync(device, url, token)
            when (result) {
                is SyncResult.Success -> {
                    settingsManager.setLastSyncTime(System.currentTimeMillis())
                    _syncState.value = SyncUIState.Success(
                        uploads = result.uploads,
                        downloads = result.downloads,
                        conflicts = result.conflicts,
                        deleted = result.deleted,
                        isSimulated = result.isSimulated
                    )
                }
                is SyncResult.Error -> {
                    _syncState.value = SyncUIState.Error(result.message)
                }
            }
        }
    }

    fun resolveConflict(id: String, resolution: String, mergedContent: String = "") {
        viewModelScope.launch {
            _syncState.value = SyncUIState.Syncing
            val device = _currentDevice.value
            repository.resolveConflict(id, device, resolution, mergedContent)
            // Trigger sync again to finalize synced state with the simulated cloud
            triggerSync()
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncUIState.Idle
    }

    // Toggle Preferences
    fun toggleFocusMode() {
        viewModelScope.launch {
            settingsManager.setFocusMode(!focusMode.value)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun setSimulatedDeviceBEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSimulatedDeviceBEnabled(enabled)
        }
    }

    fun configureSyncServer(url: String, token: String) {
        viewModelScope.launch {
            settingsManager.setSyncConfig(url, token)
        }
    }

    fun updateCustomShortcut(action: String, keyCombo: String) {
        viewModelScope.launch {
            settingsManager.updateShortcut(action, keyCombo)
        }
    }
}

sealed class SyncUIState {
    object Idle : SyncUIState()
    object Syncing : SyncUIState()
    data class Success(
        val uploads: Int,
        val downloads: Int,
        val conflicts: Int,
        val deleted: Int,
        val isSimulated: Boolean
    ) : SyncUIState()
    data class Error(val message: String) : SyncUIState()
}

class MarkdownViewModelFactory(
    private val application: Application,
    private val repository: DocumentRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkdownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarkdownViewModel(application, repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
