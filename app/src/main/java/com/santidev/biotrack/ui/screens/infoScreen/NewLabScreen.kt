package com.santidev.biotrack.ui.screens.infoScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.ui.composables.scanner.rememberDocumentScanner
import com.santidev.biotrack.ui.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLabScreen(
  viewModel: NewLabViewModel,
  onBack: () -> Unit,
  onSaved: () -> Unit,
) {
  var showDatePicker by remember { mutableStateOf(false) }
  val startScanner = rememberDocumentScanner(
    onResult = viewModel::scan,
    onError = viewModel::onScanError
  )
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = "Nuevo laboratorio") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
          }
        }
      )
    },
    bottomBar = {
      Surface(tonalElevation = 3.dp) {
        Button(
          onClick = { viewModel.save(onDone = onSaved) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
        ) { Text(text = "Guardar laboratorio", style = MaterialTheme.typography.titleMedium) }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        OutlinedButton(
          onClick = startScanner,
          enabled = !viewModel.scanning,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) {
          if (viewModel.scanning) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(text = "Leyendo el informe...")
          } else {
            Text(text = "Escanear informe")
          }
        }
      }
      if (viewModel.results.any { it.needsReview }) {
        item {
          Text(
            text = "Datos leídos del papel. Compará cada uno con el original antes de guardar; " +
              "los marcados en color son los más dudosos.",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
      item {
        OutlinedTextField(
          value = viewModel.title,
          onValueChange = viewModel::onTitleChange,
          label = { Text(text = "Título (ej. HEMOGRAMA)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
      item {
        OutlinedButton(
          onClick = { showDatePicker = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) { Text(text = "Fecha: ${formatDate(viewModel.date)}") }
      }
      items(viewModel.results, key = { it.id }) { row ->
        ResultCard(row, viewModel)
      }
      item {
        OutlinedButton(
          onClick = viewModel::addResult,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(text = "Agregar estudio")
        }
      }
      viewModel.error?.let { message ->
        item { Text(text = message, color = MaterialTheme.colorScheme.error) }
      }
    }
  }
  
  if (showDatePicker) {
    //la fecha se guarda en UTC a medianoche, que es lo mismo que usa el selector
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.date)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let(viewModel::onDateChange)
          showDatePicker = false
        }) { Text(text = "Aceptar") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancelar") }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }
}