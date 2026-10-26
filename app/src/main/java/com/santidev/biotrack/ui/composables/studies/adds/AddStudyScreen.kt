package com.santidev.biotrack.ui.composables.studies.adds

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santidev.biotrack.ui.common.localMillisToUtcMidnight
import com.santidev.biotrack.ui.composables.scanner.rememberStudyScanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyScreen(
  viewModel: AddStudyViewModel,
  onBack: () -> Unit,
  onSaved: () -> Unit,
) {
  val known by viewModel.knownParameters.collectAsStateWithLifecycle()
  val dateFormat = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "AR")) }
  var showDatePicker by remember { mutableStateOf(false) }
  val startScanner = rememberStudyScanner(
    onResult = viewModel::onScanned,
    onError = viewModel::onScanError
  )
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Nuevo estudio") },
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
          enabled = !viewModel.saving,
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(64.dp)
        ) {
          Text(
            if (viewModel.saving) "Guardando..." else "Guardar estudio",
            style = MaterialTheme.typography.titleLarge
          )
        }
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
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) { Text("Escanear estudio del papel", style = MaterialTheme.typography.titleMedium) }
      }
      item {
        OutlinedTextField(
          value = viewModel.title,
          onValueChange = viewModel::onTitleChange,
          label = { Text("Título del estudio (ej. Análisis de sangre)") },
          textStyle = MaterialTheme.typography.titleMedium,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          modifier = Modifier.fillMaxWidth()
        )
      }
      
      item {
        OutlinedButton(
          onClick = { showDatePicker = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) {
          Text(
            "Fecha: ${dateFormat.format(Date(viewModel.dateMillis))}",
            style = MaterialTheme.typography.titleMedium
          )
        }
      }
      
      if (viewModel.fromScan) {
        item {
          Text(
            "Datos leídos del papel. Compará cada uno con el original antes de guardar; " +
              "los marcados en color son los más dudosos.",
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }
      
      items(viewModel.rows, key = { it.key }) { row ->
        val isKnown = known.any { it.name.equals(row.name.trim(), ignoreCase = true) }
        val suggestions = if (row.name.isBlank() || isKnown) emptyList()
        else known.filter { it.name.contains(row.name.trim(), ignoreCase = true) }.take(4)
        
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = if (row.needsReview) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
          } else {
            CardDefaults.cardColors()
          }
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (row.needsReview) {
              Text(
                "Revisá este dato: puede estar mal leído.",
                style = MaterialTheme.typography.labelLarge
              )
            }
            OutlinedTextField(
              value = row.name,
              onValueChange = { viewModel.onNameChange(row.key, it) },
              label = { Text("Nombre (ej. Glucosa)") },
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
              modifier = Modifier.fillMaxWidth()
            )
            
            if (suggestions.isNotEmpty()) {
              Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                suggestions.forEach { parameter ->
                  SuggestionChip(
                    onClick = { viewModel.pickSuggestion(row.key, parameter) },
                    label = { Text(parameter.name) }
                  )
                }
              }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = row.value,
                onValueChange = { viewModel.onValueChange(row.key, it) },
                label = { Text("Valor") },
                supportingText = { Text("Número o texto (ej. 4,5 · <0,5 · Negativo)") },
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = row.unit,
                onValueChange = { viewModel.onUnitChange(row.key, it) },
                label = { Text("Unidad") },
                enabled = !isKnown, // si el parámetro ya existe, la unidad viene fija
                modifier = Modifier.weight(1f),
                supportingText = { Text("Ej. mg/dL, %, min, seg") },
              )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("min", "seg").forEach { time ->
                SuggestionChip(
                  onClick = { viewModel.onUnitChange(row.key, time) },
                  label = { Text("Tiempo: $time") }
                )
              }
            }
            
            TextButton(onClick = { viewModel.removeRow(row.key) }) {
              Text("Quitar parámetro")
            }
          }
        }
      }
      
      item {
        OutlinedButton(
          onClick = viewModel::addRow,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Agregar otro parámetro", style = MaterialTheme.typography.titleMedium)
        }
      }
      
      viewModel.error?.let { message ->
        item {
          Text(message, color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
  
  if (showDatePicker) {
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = localMillisToUtcMidnight(viewModel.dateMillis)
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let(viewModel::onDateSelected)
          showDatePicker = false
        }) { Text("Aceptar") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }
}