package com.santidev.biotrack.ui.screens.summaryScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.domain.SummaryItem
import com.santidev.biotrack.domain.Trend
import com.santidev.biotrack.model.display
import com.santidev.biotrack.ui.formatDate

@Composable
internal fun SummaryCard(item: SummaryItem) {
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
      if (item.result.description.isNotBlank()) {
        Text(item.result.description, style = MaterialTheme.typography.bodySmall)
      }
      
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
  Trend.UP -> Color(0xFF43A047)
  Trend.DOWN -> Color(0xFFE53935)
  Trend.SAME -> Color(0xFF1E88E5)
}

private fun Trend.message(difference: String) = when (this) {
  Trend.UP -> "Fue una suba de $difference"
  Trend.DOWN -> "Fue una baja de $difference"
  Trend.SAME -> "Sin cambios"
}