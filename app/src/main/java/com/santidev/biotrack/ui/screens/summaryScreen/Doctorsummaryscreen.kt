package com.santidev.biotrack.ui.screens.summaryScreen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.santidev.biotrack.domain.SummaryItem
import com.santidev.biotrack.domain.Trend
import com.santidev.biotrack.model.display
import com.santidev.biotrack.ui.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSummaryScreen(viewModel: DoctorSummaryViewModel, onBack: () -> Unit) {
  val items by viewModel.items.collectAsStateWithLifecycle()
  
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
    val list = items
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
          "Todavía no hay datos para mostrar.\nCargá un laboratorio primero.",
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center
        )
      }
      
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Text(
            "Valor anterior → último valor de cada estudio.",
            style = MaterialTheme.typography.bodyLarge
          )
        }
        items(list) { item -> SummaryCard(item) }
      }
    }
  }
}

@Composable
private fun SummaryCard(item: SummaryItem) {
  val comparison = item.comparison
  
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        item.result.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      
      if (comparison == null) {
        Text(
          item.result.display(),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text("Medido el ${formatDate(item.date)}", style = MaterialTheme.typography.bodyMedium)
      } else {
        val color = comparison.trend.color()
        Text(
          "${comparison.previous.display()} (${formatDate(comparison.previousDate)}) " +
            "${comparison.trend.arrow()} ${item.result.display()}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = color
        )
        Text(
          comparison.trend.message(comparison.difference),
          style = MaterialTheme.typography.titleMedium,
          color = color
        )
        Text(
          "Último valor: ${formatDate(item.date)}",
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }
  }
}

private fun Trend.arrow() = when (this) {
  Trend.UP -> "↑"
  Trend.DOWN -> "↓"
  Trend.SAME -> "→"
}

private fun Trend.color() = when (this) {
  Trend.UP -> Color(0xFF43A047)    // verde
  Trend.DOWN -> Color(0xFFE53935)  // rojo
  Trend.SAME -> Color(0xFF1E88E5)  // azul
}

private fun Trend.message(difference: String) = when (this) {
  Trend.UP -> "Fue una suba de $difference"
  Trend.DOWN -> "Fue una baja de $difference"
  Trend.SAME -> "Sin cambios"
}