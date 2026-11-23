package com.santidev.biotrack.ui.screens.infoScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.utils.ResultFields
import com.santidev.biotrack.utils.ResultForm

@Composable
fun ResultCard(row: ResultForm, viewModel: NewLabViewModel) {
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
        Text("Revisá este dato: puede estar mal leído.", style = MaterialTheme.typography.labelLarge)
      }
      ResultFields(row) { updated -> viewModel.change(row.id) { updated } }
      TextButton(onClick = { viewModel.removeResult(row.id) }) { Text("Quitar estudio") }
    }
  }
}