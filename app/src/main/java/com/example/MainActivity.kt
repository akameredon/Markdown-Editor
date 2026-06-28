package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.DocumentRepository
import com.example.data.SettingsManager
import com.example.ui.MarkdownEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MarkdownViewModel
import com.example.viewmodel.MarkdownViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Database, Settings, and Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = DocumentRepository(database.markdownDao())
    val settingsManager = SettingsManager(applicationContext)
    
    // Create ViewModel via factory
    val factory = MarkdownViewModelFactory(application, repository, settingsManager)
    val viewModel = ViewModelProvider(this, factory)[MarkdownViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = darkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MarkdownEditorScreen(viewModel = viewModel)
        }
      }
    }
  }
}
