package com.santidev.biotrack.ui.screens.initialScreen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.model.LabResult
import com.santidev.biotrack.model.display
import com.santidev.biotrack.ui.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabListScreen(
  viewModel: LabListViewModel,
  onNewLab: () -> Unit,
  onOpenSummary: () -> Unit,
  onSignOut: () -> Unit,
) {
  val labs by viewModel.labs.collectAsStateWithLifecycle()
  var showConfirm by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<Pair<Lab, Int>?>(null) }
  
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
          title = { Text("Mis laboratorios") },
          actions = {
            IconButton(onClick = viewModel::startSelection) {
              Icon(Icons.Default.Delete, contentDescription = "Borrar laboratorios")
            }
            TextButton(onClick = onSignOut) { Text("Cerrar sesión") }
          }
        )
      }
    },
    floatingActionButton = {
      if (!viewModel.selectionMode) {
        ExtendedFloatingActionButton(
          onClick = onNewLab,
          icon = { Icon(Icons.Default.Add, contentDescription = null) },
          text = { Text("Nuevo laboratorio") }
        )
      }
    }
  ) { padding ->
    val list = labs
    when {
      list == null -> Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
      ) { CircularProgressIndicator() }
      
      list.isEmpty() -> Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "Todavía no hay laboratorios.\nTocá \"Nuevo laboratorio\" para empezar.",
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center
        )
      }
      
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (!viewModel.selectionMode) {
          item {
            FilledTonalButton(
              onClick = onOpenSummary,
              modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
              Text("Resumen para el médico", style = MaterialTheme.typography.titleLarge)
            }
          }
        }
        items(list, key = { it.id }) { lab ->
          LabCard(
            lab = lab,
            selectionMode = viewModel.selectionMode,
            selected = lab.id in viewModel.selectedIds,
            onSelectedChange = { viewModel.toggle(lab.id, it) },
            onEdit = { index -> editing = lab to index }
          )
        }
      }
    }
  }
  
  editing?.let { (lab, index) ->
    EditResultDialog(
      result = lab.results[index],
      onSave = { result ->
        viewModel.updateResult(lab, index, result)
        editing = null
      },
      onDismiss = { editing = null }
    )
  }
  
  if (showConfirm) {
    val n = viewModel.selectedIds.size
    AlertDialog(
      onDismissRequest = { showConfirm = false },
      title = { Text("¿Borrar laboratorios?") },
      text = {
        Text(
          "Se ${if (n == 1) "va a borrar 1 laboratorio" else "van a borrar $n laboratorios"} " +
            "con todos sus estudios. Esto no se puede deshacer."
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

// Al tocar la tarjeta se muestran u ocultan los estudios.
// En modo borrar, tocarla (o usar el switch) la selecciona.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabCard(
  lab: Lab,
  selectionMode: Boolean,
  selected: Boolean,
  onSelectedChange: (Boolean) -> Unit,
  onEdit: (Int) -> Unit
) {
  var expanded by rememberSaveable(lab.id) { mutableStateOf(false) }
  
  Card(
    onClick = { if (selectionMode) onSelectedChange(!selected) else expanded = !expanded },
    modifier = Modifier.fillMaxWidth(),
    colors = if (selected) {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
      CardDefaults.cardColors()
    }
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(lab.title, style = MaterialTheme.typography.titleLarge)
          Text(formatDate(lab.date), style = MaterialTheme.typography.bodyMedium)
        }
        if (selectionMode) {
          Switch(checked = selected, onCheckedChange = onSelectedChange)
        }
      }
      if (expanded && !selectionMode) {
        Spacer(Modifier.height(8.dp))
        lab.results.forEachIndexed { index, result ->
          ResultLine(result, onEdit = { onEdit(index) })
        }
      }
    }
  }
}

@Composable
private fun ResultLine(result: LabResult, onEdit: () -> Unit) {
  Row(
    modifier = Modifier.padding(bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(result.name, style = MaterialTheme.typography.titleMedium)
      Text(result.display(), style = MaterialTheme.typography.bodyLarge)
    }
    IconButton(onClick = onEdit) {
      Icon(Icons.Default.Edit, contentDescription = "Editar estudio")
    }
  }
}