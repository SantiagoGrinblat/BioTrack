package com.santidev.biotrack.ui.screens.summaryScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.domain.SummaryFolder

//se toca para abrirla y poder ver sus estudios
@Composable
internal fun FolderSection(folder: SummaryFolder) {
  var expanded by rememberSaveable(folder.name) { mutableStateOf(false) }
  
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
      Row(
        modifier = Modifier.padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            folder.name.ifEmpty { "Sin carpeta" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            if (folder.items.size == 1) "1 estudio" else "${folder.items.size} estudios",
            style = MaterialTheme.typography.bodyMedium
          )
        }
        Icon(
          if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
          contentDescription = if (expanded) "Cerrar carpeta" else "Abrir carpeta"
        )
      }
    }
    if (expanded) {
      folder.items.forEach { item -> SummaryCard(item) }
    }
  }
}