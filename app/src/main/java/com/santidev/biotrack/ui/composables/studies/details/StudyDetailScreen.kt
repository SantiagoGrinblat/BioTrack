package com.santidev.biotrack.ui.composables.studies.details

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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDetailScreen(viewModel: StudyDetailViewModel, onBack: () -> Unit) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val dateFormat = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "AR")) }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Detalle del estudio") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
          }
        }
      )
    }
  ) { padding ->
    when (val s = state) {
      StudyDetailState.Loading -> Box(
        Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) { CircularProgressIndicator() }
      
      StudyDetailState.NotFound, StudyDetailState.Error -> Box(
        Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          if (s == StudyDetailState.NotFound) "No se encontró el estudio."
          else "No se pudo cargar el estudio.",
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Center
        )
      }
      
      is StudyDetailState.Ready -> LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Column(Modifier.padding(bottom = 8.dp)) {
            Text(s.study.title, style = MaterialTheme.typography.headlineMedium)
            Text(
              dateFormat.format(s.study.date.toDate()),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        items(s.items, key = { it.current.id }) { item ->
          MeasurementCard(item, dateFormat)
        }
      }
    }
  }
}

@Composable
private fun MeasurementCard(item: MeasurementDetail, dateFormat: SimpleDateFormat) {
  val unit = item.parameter?.unit.orEmpty()
  val colors = if (item.outOfRange) {
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  } else {
    CardDefaults.cardColors()
  }
  
  Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(item.parameter?.name ?: "Parámetro", style = MaterialTheme.typography.titleLarge)
      Text(
        "${item.current.displayValue()} $unit".trim(),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
      )
      
      
      if (item.outOfRange) {
        Text(
          "⚠ Fuera de rango",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.error
        )
      }
      
      // Si hay un solo dato (no hay anterior), se muestra solo el valor
      item.previous?.let { previous ->
        Text(
          "Anterior: ${previous.displayValue()} $unit · ${dateFormat.format(previous.date.toDate())}".trim(),
          style = MaterialTheme.typography.bodyLarge
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
      }
    }
  }
}