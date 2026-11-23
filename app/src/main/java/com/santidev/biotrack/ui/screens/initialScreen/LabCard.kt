package com.santidev.biotrack.ui.screens.initialScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.ui.formatDate

//al tocar la tarjeta se muestran u ocultan los estudios.
//en modo borrar, tocarla (o usar el switch) la selecciona.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabCard(
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
          Text(text = lab.title, style = MaterialTheme.typography.titleLarge)
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