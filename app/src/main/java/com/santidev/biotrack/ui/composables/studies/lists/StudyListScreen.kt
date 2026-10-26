package com.santidev.biotrack.ui.composables.studies.lists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyListScreen(
  viewModel: StudyListViewModel,
  onAddStudy: () -> Unit,
  onOpenStudy: (String) -> Unit,
  onSignOut: () -> Unit,
  onOpenSummary: () -> Unit,
) {
  val studies by viewModel.studies.collectAsStateWithLifecycle()
  val dateFormat = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "AR")) }
  var showConfirm by remember { mutableStateOf(false) }
  
  BackHandler(enabled = viewModel.selectionMode) { viewModel.cancelSelection() }
  
  Scaffold(
    topBar = {
      if (viewModel.selectionMode) {
        TopAppBar(
          title = {
            val n = viewModel.selectedIds.size
            Text(if (n == 1) "1 seleccionado" else "$n seleccionados")
          },
          navigationIcon = {
            IconButton(onClick = viewModel::cancelSelection) {
              Icon(Icons.Default.Close, contentDescription = "Cancelar")
            }
          },
          actions = {
            IconButton(
              onClick = { showConfirm = true },
              enabled = viewModel.selectedIds.isNotEmpty()
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Borrar seleccionados")
            }
          }
        )
      } else {
        TopAppBar(
          title = { Text("Mis estudios") },
          actions = {
            IconButton(onClick = viewModel::startSelection) {
              Icon(Icons.Default.Delete, contentDescription = "Borrar estudios")
            }
            TextButton(onClick = onSignOut) { Text("Cerrar sesión") }
          }
        )
      }
    },
    floatingActionButton = {
      if (!viewModel.selectionMode) {
        ExtendedFloatingActionButton(
          onClick = onAddStudy,
          icon = { Icon(Icons.Default.Add, contentDescription = null) },
          text = { Text("Agregar estudio", style = MaterialTheme.typography.titleLarge) }
        )
      }
    }
  ) { padding ->
    when {
      studies == null -> Box(
        Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) { CircularProgressIndicator() }
      
      studies!!.isEmpty() -> Box(
        Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "Todavía no hay estudios cargados.\nTocá \"Agregar estudio\" para empezar.",
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Center
        )
      }
      
      else -> LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          FilledTonalButton(
            onClick = onOpenSummary,
            modifier = Modifier.fillMaxWidth().height(64.dp)
          ) {
            Text("Resumen para el médico", style = MaterialTheme.typography.titleLarge)
          }
        }
        items(studies!!, key = { it.id }) { study ->
          val selected = study.id in viewModel.selectedIds
          Card(
            onClick = {
              if (viewModel.selectionMode) viewModel.toggle(study.id)
              else onOpenStudy(study.id)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (selected) {
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
              CardDefaults.cardColors()
            }
          ) {
            Row(
              modifier = Modifier.padding(20.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (viewModel.selectionMode) {
                Checkbox(
                  checked = selected,
                  onCheckedChange = { viewModel.toggle(study.id) }
                )
                Spacer(Modifier.width(12.dp))
              }
              Column {
                Text(study.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                  dateFormat.format(study.date.toDate()),
                  style = MaterialTheme.typography.bodyLarge
                )
              }
            }
          }
        }
      }
    }
  }
  if (showConfirm) {
    val n = viewModel.selectedIds.size
    AlertDialog(
      onDismissRequest = { showConfirm = false },
      title = { Text("¿Borrar estudios?") },
      text = {
        Text(
          "Se ${if (n == 1) "va a borrar 1 estudio" else "van a borrar $n estudios"} " +
            "con todos sus valores. Esto no se puede deshacer."
        )
      },
      confirmButton = {
        TextButton(onClick = {
          showConfirm = false
          viewModel.deleteSelected()
        }) { Text("Borrar") }
      },
      dismissButton = {
        TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
      }
    )
  }
}