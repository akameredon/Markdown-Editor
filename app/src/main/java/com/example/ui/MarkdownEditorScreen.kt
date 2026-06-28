package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.scale
import com.example.ui.TableOfContentsDialog
import com.example.ui.SpellCheckDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarkdownDocument
import com.example.viewmodel.MarkdownViewModel
import com.example.viewmodel.SyncUIState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorScreen(
    viewModel: MarkdownViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val activeDocument by viewModel.activeDocument.collectAsState()
    val activeDocsList by viewModel.activeDocuments.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val focusMode by viewModel.focusMode.collectAsState()
    val simulatedDeviceBEnabled by viewModel.simulatedDeviceBEnabled.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    // Preferences and Shortcuts
    val themeMode by viewModel.themeMode.collectAsState()
    val shortcutBold by viewModel.shortcutBold.collectAsState()
    val shortcutItalic by viewModel.shortcutItalic.collectAsState()
    val shortcutPreview by viewModel.shortcutPreview.collectAsState()
    val shortcutFocus by viewModel.shortcutFocus.collectAsState()
    val shortcutSync by viewModel.shortcutSync.collectAsState()

    val syncServerUrl by viewModel.syncServerUrl.collectAsState()
    val syncApiToken by viewModel.syncApiToken.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    val activeVersions by viewModel.activeVersions.collectAsState()

    // Dialog sheets states
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var activeConflictPair by remember { mutableStateOf<Pair<MarkdownDocument, MarkdownDocument>?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTocDialog by remember { mutableStateOf(false) }
    var showSpellCheckDialog by remember { mutableStateOf(false) }

    // Settings states
    val autoSaveEnabled by viewModel.autoSaveEnabled.collectAsState()
    val customDictionary by viewModel.customDictionary.collectAsState()

    // Editor state holding text
    var editorTitle by remember { mutableStateOf("") }
    var editorContentValue by remember { mutableStateOf(TextFieldValue("")) }

    // Synchronize local states when active document changes
    LaunchedEffect(activeDocument) {
        val doc = activeDocument
        if (doc != null) {
            editorTitle = doc.title
            if (editorContentValue.text != doc.content) {
                editorContentValue = TextFieldValue(
                    text = doc.content,
                    selection = TextRange(doc.content.length)
                )
            }
        } else {
            editorTitle = ""
            editorContentValue = TextFieldValue("")
        }
    }

    // Callback when text is edited in real time
    fun onTextChange(newValue: TextFieldValue) {
        editorContentValue = newValue
        viewModel.updateDocumentContent(editorTitle, newValue.text)
    }

    fun onTitleChange(newTitle: String) {
        editorTitle = newTitle
        viewModel.updateDocumentContent(newTitle, editorContentValue.text)
    }

    // Markdown insertion helper
    fun insertMarkdownFormatting(prefix: String, suffix: String) {
        val text = editorContentValue.text
        val selection = editorContentValue.selection
        val start = selection.start
        val end = selection.end
        val selectedText = text.substring(start, end)
        
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        val newSelectionStart = start + prefix.length
        val newSelectionEnd = newSelectionStart + selectedText.length
        
        val updatedValue = TextFieldValue(
            text = newText,
            selection = TextRange(newSelectionStart, newSelectionEnd)
        )
        onTextChange(updatedValue)
    }

    // Monitor sync states to check for conflicts
    LaunchedEffect(syncState) {
        if (syncState is SyncUIState.Success) {
            val state = syncState as SyncUIState.Success
            if (state.conflicts > 0) {
                // Find conflicting document and display the Conflict Resolution Dialog
                val conflictingDoc = activeDocsList.firstOrNull { it.isConflict() }
                if (conflictingDoc != null) {
                    val cloudDoc = viewModel.activeDocuments.value.firstOrNull { it.id == conflictingDoc.id }
                        ?: viewModel.getDocumentById(conflictingDoc.id, "CLOUD")
                    if (cloudDoc != null) {
                        activeConflictPair = conflictingDoc to cloudDoc
                    }
                }
            }
        }
    }

    // Keyboard Shortcuts Global Key Listener
    val focusRequester = remember { FocusRequester() }
    
    fun handleShortcutEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        
        // Helper to match combinations
        fun isMatch(shortcutStr: String): Boolean {
            val parts = shortcutStr.split("+").map { it.trim().lowercase() }
            val needsCtrl = "ctrl" in parts
            val needsAlt = "alt" in parts
            val needsShift = "shift" in parts
            
            val hasCtrl = event.isCtrlPressed
            val hasAlt = event.isAltPressed
            val hasShift = event.isShiftPressed
            
            if (needsCtrl != hasCtrl) return false
            if (needsAlt != hasAlt) return false
            if (needsShift != hasShift) return false
            
            val keyName = parts.last()
            return when (keyName) {
                "b" -> event.key == Key.B
                "i" -> event.key == Key.I
                "p" -> event.key == Key.P
                "f" -> event.key == Key.F
                "s" -> event.key == Key.S
                else -> false
            }
        }

        return when {
            isMatch(shortcutBold) -> {
                insertMarkdownFormatting("**", "**")
                true
            }
            isMatch(shortcutItalic) -> {
                insertMarkdownFormatting("*", "*")
                true
            }
            isMatch(shortcutPreview) -> {
                val nextMode = when (viewMode) {
                    "EDIT" -> "SPLIT"
                    "SPLIT" -> "PREVIEW"
                    else -> "EDIT"
                }
                viewModel.setViewMode(nextMode)
                true
            }
            isMatch(shortcutFocus) -> {
                viewModel.toggleFocusMode()
                true
            }
            isMatch(shortcutSync) -> {
                viewModel.triggerSync()
                true
            }
            else -> false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { handleShortcutEvent(it) }
    ) {
        // Main Screen Content
        Row(modifier = Modifier.fillMaxSize()) {
            
            // Collapsible Sidebar (Only visible when NOT in Focus Mode)
            var sidebarVisible by remember { mutableStateOf(true) }
            
            if (!focusMode) {
                AnimatedVisibility(
                    visible = sidebarVisible,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header with New File Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { showTemplateDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Create note")
                                }
                            }

                            // Device Selector (Workspace Sync Simulator Switcher)
                            if (simulatedDeviceBEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .padding(4.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setDevice("DEVICE_A") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentDevice == "DEVICE_A") MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (currentDevice == "DEVICE_A") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Device A", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.setDevice("DEVICE_B") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentDevice == "DEVICE_B") MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            contentColor = if (currentDevice == "DEVICE_B") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Device B", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Documents List
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (activeDocsList.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Article,
                                                    contentDescription = "Empty Notes",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "No documents yet",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(activeDocsList) { doc ->
                                        val isSelected = doc.id == (if (currentDevice == "DEVICE_A") viewModel.selectedDocIdA.value else viewModel.selectedDocIdB.value)
                                        
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.selectDocument(doc.id) }
                                                .border(
                                                    if (isSelected) 1.dp else 0.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = doc.title.ifBlank { "Untitled Note" },
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "v${doc.version}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        // Badge for sync status
                                                        val statusColor = when (doc.syncStatus) {
                                                            "SYNCED" -> Color(0xFF4CAF50)
                                                            "CONFLICT" -> MaterialTheme.colorScheme.error
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(statusColor)
                                                        )
                                                        Text(
                                                            text = doc.syncStatus,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = statusColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteDocument(doc.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                
                                Button(
                                    onClick = { viewModel.triggerSync() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync Documents")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { showShortcutsDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Keyboard, contentDescription = "Shortcuts")
                                    }
                                    IconButton(
                                        onClick = { showSettingsSheet = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Main Editor Panel Area
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    if (!focusMode) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = if (simulatedDeviceBEnabled) {
                                            "Workspace: $currentDevice" + (activeDocument?.title?.let { " - $it" } ?: "")
                                        } else {
                                            activeDocument?.title?.ifBlank { "Untitled Note" } ?: "Markdown Editor"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    
                                    // Immersive subtitle sync indicator matching HTML
                                    val (syncText, syncIcon, syncColor) = when (syncState) {
                                        is SyncUIState.Syncing -> Triple("Syncing...", Icons.Default.Sync, MaterialTheme.colorScheme.primary)
                                        is SyncUIState.Success -> Triple("Synced to Cloud", Icons.Default.CloudDone, MaterialTheme.colorScheme.primary)
                                        is SyncUIState.Error -> Triple("Sync Failed", Icons.Default.CloudOff, MaterialTheme.colorScheme.error)
                                        else -> Triple("Saved Offline", Icons.Default.CloudQueue, MaterialTheme.colorScheme.secondary)
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = syncIcon,
                                            contentDescription = null,
                                            tint = syncColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = syncText.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = syncColor
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { sidebarVisible = !sidebarVisible }) {
                                    Icon(
                                        imageVector = if (sidebarVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                                        contentDescription = "Toggle Menu"
                                    )
                                }
                            },
                            actions = {
                                // Editor/Preview Mode Buttons
                                SingleChoiceSegmentedButtonRow {
                                    SegmentedButton(
                                        selected = viewMode == "EDIT",
                                        onClick = { viewModel.setViewMode("EDIT") },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                                    ) {
                                        Text("Edit")
                                    }
                                    SegmentedButton(
                                        selected = viewMode == "SPLIT",
                                        onClick = { viewModel.setViewMode("SPLIT") },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                                    ) {
                                        Text("Split")
                                    }
                                    SegmentedButton(
                                        selected = viewMode == "PREVIEW",
                                        onClick = { viewModel.setViewMode("PREVIEW") },
                                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                                    ) {
                                        Text("Preview")
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                IconButton(
                                    onClick = { showHistoryDialog = true },
                                    enabled = activeDocument != null,
                                    modifier = Modifier.testTag("topbar_history_button")
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "Version History")
                                }

                                IconButton(
                                    onClick = { showExportDialog = true },
                                    enabled = activeDocument != null,
                                    modifier = Modifier.testTag("topbar_export_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Export Document")
                                }

                                IconButton(onClick = { viewModel.toggleFocusMode() }) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Enter Focus Mode")
                                }
                            }
                        )
                    }
                }
            ) { paddingValues ->
                // Editor Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (activeDocument == null) {
                        // Empty State Welcome Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = "Welcome logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(96.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Write freely, sync seamlessly",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Create a new document or load an existing markdown file in your local workspace. All changes are stored offline first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.createNewDocument() },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Icon")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("New Markdown Note")
                                }
                            }
                        }
                    } else {
                        // Formatting Shortcut Buttons (Hidden in Focus Mode)
                        if (!focusMode) {
                            ScrollableTabRow(
                                selectedTabIndex = 0,
                                indicator = {},
                                divider = {},
                                edgePadding = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                val shortcuts = listOf(
                                    "Bold" to { insertMarkdownFormatting("**", "**") },
                                    "Italic" to { insertMarkdownFormatting("*", "*") },
                                    "H1" to { insertMarkdownFormatting("# ", "") },
                                    "H2" to { insertMarkdownFormatting("## ", "") },
                                    "H3" to { insertMarkdownFormatting("### ", "") },
                                    "Code" to { insertMarkdownFormatting("`", "`") },
                                    "Code Block" to { insertMarkdownFormatting("```\n", "\n```") },
                                    "Quote" to { insertMarkdownFormatting("> ", "") },
                                    "List" to { insertMarkdownFormatting("- ", "") },
                                    "Divider" to { insertMarkdownFormatting("\n---\n", "") }
                                )
                                shortcuts.forEach { (label, action) ->
                                    Button(
                                        onClick = action,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                            .height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Writing Area - Adaptive Layout: Row for wide screens, Column for compact mobile screens
                        val configuration = LocalConfiguration.current
                        val isCompactScreen = configuration.screenWidthDp < 600

                        if (isCompactScreen) {
                            Column(modifier = Modifier.weight(1f)) {
                                // EDIT PANE
                                if (viewMode == "EDIT" || viewMode == "SPLIT") {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(if (focusMode) 16.dp else 12.dp)
                                    ) {
                                        // Live Title Field (No label, giant minimalist size)
                                        TextField(
                                            value = editorTitle,
                                            onValueChange = { onTitleChange(it) },
                                            placeholder = { Text("Title", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                                            textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // Content Field
                                        TextField(
                                            value = editorContentValue,
                                            onValueChange = { onTextChange(it) },
                                            placeholder = { Text("Type some markdown format here...", fontFamily = FontFamily.Monospace) },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        )
                                    }
                                }

                                // Divider for split screen view
                                if (viewMode == "SPLIT") {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }

                                // PREVIEW PANE
                                if (viewMode == "PREVIEW" || viewMode == "SPLIT") {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                    ) {
                                        MarkdownPreview(
                                            markdown = editorContentValue.text,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(modifier = Modifier.weight(1f)) {
                                // EDIT PANE
                                if (viewMode == "EDIT" || viewMode == "SPLIT") {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(if (focusMode) 24.dp else 12.dp)
                                    ) {
                                        // Live Title Field (No label, giant minimalist size)
                                        TextField(
                                            value = editorTitle,
                                            onValueChange = { onTitleChange(it) },
                                            placeholder = { Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                                            textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // Content Field
                                        TextField(
                                            value = editorContentValue,
                                            onValueChange = { onTextChange(it) },
                                            placeholder = { Text("Type some markdown format here...", fontFamily = FontFamily.Monospace) },
                                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        )
                                    }
                                }

                                // Divider for split screen view
                                if (viewMode == "SPLIT") {
                                    VerticalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }

                                // PREVIEW PANE
                                if (viewMode == "PREVIEW" || viewMode == "SPLIT") {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                    ) {
                                        MarkdownPreview(
                                            markdown = editorContentValue.text,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Modern Status Bar (Word Count, Auto Save toggle & state, Table of Contents, Spell Check)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (focusMode) 40.dp else 48.dp)
                                .testTag("editor_status_bar"),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            val text = editorContentValue.text
                            val wordCount = remember(text) {
                                if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
                            }
                            val charCount = text.length
                            val readingTime = maxOf(1, (wordCount / 200) + if (wordCount % 200 > 0) 1 else 0)

                            if (isCompactScreen) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Just Compact Word count
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "$wordCount words",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Center: Compact Auto-Save status & Switch (saving horizontal space)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        var isSavingLocalSimulated by remember { mutableStateOf(false) }
                                        LaunchedEffect(editorContentValue.text) {
                                            if (autoSaveEnabled && editorContentValue.text.isNotEmpty()) {
                                                isSavingLocalSimulated = true
                                                kotlinx.coroutines.delay(600)
                                                isSavingLocalSimulated = false
                                            }
                                        }

                                        if (autoSaveEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        color = if (isSavingLocalSimulated) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                                        shape = androidx.compose.foundation.shape.CircleShape
                                                    )
                                            )
                                            Text(
                                                text = if (isSavingLocalSimulated) "Saving" else "Saved",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = androidx.compose.foundation.shape.CircleShape
                                                    )
                                            )
                                            Button(
                                                onClick = { viewModel.saveActiveDocumentManual() },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                modifier = Modifier.height(20.dp).testTag("manual_save_button"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Text("Save", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Switch(
                                            checked = autoSaveEnabled,
                                            onCheckedChange = { viewModel.setAutoSaveEnabled(it) },
                                            modifier = Modifier
                                                .scale(0.55f)
                                                .height(14.dp)
                                                .testTag("autosave_switch")
                                        )
                                    }

                                    // Right: Compact Dialog icons (TOC & Spell Check)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showTocDialog = true },
                                            modifier = Modifier.size(28.dp).testTag("toc_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Toc,
                                                contentDescription = "Table of Contents",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { showSpellCheckDialog = true },
                                            modifier = Modifier.size(28.dp).testTag("spellcheck_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Spellcheck,
                                                contentDescription = "Spell Checker",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left Side: Word and Char counts & Reading time
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "$wordCount words   •   $charCount chars   •   $readingTime min read",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Middle Side: Auto-Save indicator & settings
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        var isSavingLocalSimulated by remember { mutableStateOf(false) }
                                        
                                        // Trigger a brief reactive indicator on character modification
                                        LaunchedEffect(editorContentValue.text) {
                                            if (autoSaveEnabled && editorContentValue.text.isNotEmpty()) {
                                                isSavingLocalSimulated = true
                                                kotlinx.coroutines.delay(600)
                                                isSavingLocalSimulated = false
                                            }
                                        }

                                        if (autoSaveEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        color = if (isSavingLocalSimulated) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                                        shape = androidx.compose.foundation.shape.CircleShape
                                                    )
                                            )
                                            Text(
                                                text = if (isSavingLocalSimulated) "Saving..." else "Saved",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = androidx.compose.foundation.shape.CircleShape
                                                    )
                                            )
                                            Text(
                                                text = "Unsaved",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.saveActiveDocumentManual()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp).testTag("manual_save_button"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Save,
                                                    contentDescription = "Save",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Save", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Auto-save",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Switch(
                                            checked = autoSaveEnabled,
                                            onCheckedChange = { viewModel.setAutoSaveEnabled(it) },
                                            modifier = Modifier
                                                .scale(0.6f)
                                                .height(16.dp)
                                                .testTag("autosave_switch")
                                        )
                                    }

                                    // Right Side: Table of Contents & Spellcheck Dialog triggers
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showTocDialog = true },
                                            modifier = Modifier.size(32.dp).testTag("toc_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Toc,
                                                contentDescription = "Table of Contents",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { showSpellCheckDialog = true },
                                            modifier = Modifier.size(32.dp).testTag("spellcheck_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Spellcheck,
                                                contentDescription = "Spell Checker",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action buttons for Focus Mode exit & shortcut reminder
        if (focusMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.toggleFocusMode() },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Focus Mode")
                }
            }
        }

        // Standard bottom popup notification for background synchronization events
        val context = LocalContext.current
        var lastStateMessage by remember { mutableStateOf<String?>(null) }
        var showSnack by remember { mutableStateOf(false) }

        LaunchedEffect(syncState) {
            when (syncState) {
                is SyncUIState.Syncing -> {
                    lastStateMessage = "Syncing with Cloud..."
                    showSnack = true
                }
                is SyncUIState.Success -> {
                    val info = syncState as SyncUIState.Success
                    lastStateMessage = if (info.isSimulated) {
                        "Simulated Sync completed! uploaded: ${info.uploads}, downloaded: ${info.downloads}, conflicts: ${info.conflicts}"
                    } else {
                        "Cloud Sync completed! uploaded: ${info.uploads}, downloaded: ${info.downloads}"
                    }
                    showSnack = true
                }
                is SyncUIState.Error -> {
                    lastStateMessage = "Sync failed: ${(syncState as SyncUIState.Error).message}"
                    showSnack = true
                }
                else -> {
                    showSnack = false
                }
            }
        }

        if (showSnack && lastStateMessage != null) {
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.clearSyncState() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.inversePrimary)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = lastStateMessage!!)
            }
        }

        // Custom shortcuts dialog
        if (showShortcutsDialog) {
            ShortcutsDialog(
                shortcutBold = shortcutBold,
                shortcutItalic = shortcutItalic,
                shortcutPreview = shortcutPreview,
                shortcutFocus = shortcutFocus,
                shortcutSync = shortcutSync,
                onSaveShortcut = { action, binding -> viewModel.updateCustomShortcut(action, binding) },
                onDismiss = { showShortcutsDialog = false }
            )
        }

        // Settings Modal Bottom Sheet Dialog
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Theme selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Theme Style", fontWeight = FontWeight.Bold)
                            Text("Switch between Light/Dark display", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = themeMode == "DARK",
                                onClick = { viewModel.setThemeMode("DARK") },
                                label = { Text("Dark") }
                            )
                            FilterChip(
                                selected = themeMode == "LIGHT",
                                onClick = { viewModel.setThemeMode("LIGHT") },
                                label = { Text("Light") }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Simulated Device B toggle for Playground conflict testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulated Multi-Device Mode", fontWeight = FontWeight.Bold)
                            Text("Spins up a 'Device B' workspace. Test offline changes, cloud updates, and conflict resolution directly!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = simulatedDeviceBEnabled,
                            onCheckedChange = { viewModel.setSimulatedDeviceBEnabled(it) }
                        )
                    }

                    HorizontalDivider()

                    // Cloud Server sync parameters
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cloud Sync Server Config (Optional)", fontWeight = FontWeight.Bold)
                        Text("Enter your custom REST Server sync endpoint to sync with real hardware devices.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        var inputUrl by remember { mutableStateOf(syncServerUrl) }
                        var inputToken by remember { mutableStateOf(syncApiToken) }

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("https://my-server.com/api/sync") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputToken,
                            onValueChange = { inputToken = it },
                            label = { Text("API Token / Auth Secret") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.configureSyncServer(inputUrl.trim(), inputToken.trim())
                                showSettingsSheet = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Configurations")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Conflict Resolution Dialog Overlay
        val conflictPair = activeConflictPair
        if (conflictPair != null) {
            ConflictResolutionDialog(
                localDoc = conflictPair.first,
                cloudDoc = conflictPair.second,
                onResolve = { resolution, merged ->
                    viewModel.resolveConflict(conflictPair.first.id, resolution, merged)
                    activeConflictPair = null
                },
                onDismiss = { activeConflictPair = null }
            )
        }

        // Template Selection Dialog Overlay
        if (showTemplateDialog) {
            TemplateSelectionDialog(
                onDismiss = { showTemplateDialog = false },
                onSelectBlank = { viewModel.createNewDocument() },
                onSelectTemplate = { template -> viewModel.createNewDocumentFromTemplate(template) }
            )
        }

        // Version History Dialog Overlay
        if (showHistoryDialog) {
            VersionHistoryDialog(
                versions = activeVersions,
                onDismiss = { showHistoryDialog = false },
                onCreateSnapshot = { label -> viewModel.createVersionSnapshot(label) },
                onRestore = { version -> viewModel.restoreVersion(version) }
            )
        }

        // Document Export Dialog Overlay
        if (showExportDialog) {
            activeDocument?.let { doc ->
                ExportDialog(
                    documentTitle = doc.title,
                    documentContent = doc.content,
                    onDismiss = { showExportDialog = false }
                )
            }
        }

        // Table of Contents Dialog Overlay
        if (showTocDialog) {
            TableOfContentsDialog(
                documentContent = editorContentValue.text,
                onDismiss = { showTocDialog = false },
                onHeaderClick = { index ->
                    editorContentValue = TextFieldValue(
                        text = editorContentValue.text,
                        selection = TextRange(index)
                    )
                }
            )
        }

        // Spell Checker Dialog Overlay
        if (showSpellCheckDialog) {
            SpellCheckDialog(
                documentContent = editorContentValue.text,
                customDictionary = customDictionary,
                onDismiss = { showSpellCheckDialog = false },
                onAddWordToDictionary = { word -> viewModel.addWordToDictionary(word) },
                onReplaceWord = { startIndex, endIndex, replacement ->
                    val originalText = editorContentValue.text
                    val newText = originalText.substring(0, startIndex) + replacement + originalText.substring(endIndex)
                    val diff = replacement.length - (endIndex - startIndex)
                    val newSelectionIndex = minOf(newText.length, maxOf(0, editorContentValue.selection.start + diff))
                    
                    val updatedValue = TextFieldValue(
                        text = newText,
                        selection = TextRange(newSelectionIndex)
                    )
                    onTextChange(updatedValue)
                }
            )
        }
    }

    // Capture focus to capture keyboard events immediately
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
