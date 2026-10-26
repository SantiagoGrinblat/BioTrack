package com.santidev.biotrack.ui.composables.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santidev.biotrack.ui.common.DownColor
import com.santidev.biotrack.ui.common.EqualColor
import com.santidev.biotrack.ui.common.UpColor
import com.santidev.biotrack.ui.common.displayValue
import com.santidev.biotrack.ui.common.formatNumber
import com.santidev.biotrack.ui.composables.studies.details.MeasurementDetail
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSummaryScreen(viewModel: DoctorSummaryViewModel, onBack: () -> Unit) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale("es", "AR")) }
  
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
    when (val s = state) {
      SummaryState.Loading -> Box(
        Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) { CircularProgressIndicator() }
      
      SummaryState.Error -> Box(
        Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "No se pudo cargar el resumen.",
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Center
        )
      }
      
      is SummaryState.Ready ->
        if (s.items.isEmpty()) {
          Box(
            Modifier
              .fillMaxSize()
              .padding(padding)
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "Todavía no hay datos para mostrar.\nCargá un estudio primero.",
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Center
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            item {
              Text(
                "Valor anterior → último valor, de cada parámetro.",
                style = MaterialTheme.typography.bodyLarge
              )
            }
            items(s.items, key = { it.current.id }) { item ->
              SummaryCard(item, dateFormat)
            }
          }
        }
    }
  }
}

@Composable
private fun SummaryCard(item: MeasurementDetail, dateFormat: SimpleDateFormat) {
  val unit = item.parameter?.unit.orEmpty()
  val previous = item.previous
  val colors = if (item.outOfRange) {
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  } else {
    CardDefaults.cardColors()
  }
  
  Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        item.parameter?.name ?: "Parámetro",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      
      // "92 → 105 mg/dL", o solo "105 mg/dL" si hay un único dato
      val values = if (previous != null) {
        "${previous.displayValue()} → ${item.current.displayValue()} $unit"
      } else {
        "${item.current.displayValue()} $unit"
      }
      Text(
        values.trim(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      
      item.difference?.let { difference ->
        val (text, color) = when {
          abs(difference) < 1e-9 -> "→ Sin cambios" to EqualColor
          difference > 0 -> "↑ Subió ${formatNumber(difference)} $unit" to UpColor
          else -> "↓ Bajó ${formatNumber(abs(difference))} $unit" to DownColor
        }
        Text(
          text.trim(),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = color
        )
      }
      
      if (item.outOfRange) {
        Text(
          "⚠ Fuera de rango",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.error
        )
      }
      
      Text(
        if (previous != null) {
          "${dateFormat.format(previous.date.toDate())} → ${dateFormat.format(item.current.date.toDate())}"
        } else {
          "Medido el ${dateFormat.format(item.current.date.toDate())}"
        },
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}