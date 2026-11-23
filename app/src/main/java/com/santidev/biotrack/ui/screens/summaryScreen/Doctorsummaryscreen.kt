package com.santidev.biotrack.ui.screens.summaryScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSummaryScreen(viewModel: DoctorSummaryViewModel, onBack: () -> Unit) {
  val folders by viewModel.items.collectAsStateWithLifecycle()
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Resumen para el médico") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
          }
        }
      )
    }
  ) { padding ->
    val list = folders
    when {
      list == null -> Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) { CircularProgressIndicator() }
      
      list.isEmpty() -> Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "Todavía no hay datos para mostrar.\nCargá un laboratorio primero.",
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center
        )
      }
      
      else -> LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Text(
            "Valor anterior → último valor de cada estudio.",
            style = MaterialTheme.typography.bodyLarge
          )
        }
        items(list, key = { it.name }) { folder -> FolderSection(folder) }
      }
    }
  }
}