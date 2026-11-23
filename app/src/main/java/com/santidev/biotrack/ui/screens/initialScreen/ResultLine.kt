package com.santidev.biotrack.ui.screens.initialScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.model.LabResult
import com.santidev.biotrack.model.display

@Composable
fun ResultLine(result: LabResult, onEdit: () -> Unit) {
  Row(
    modifier = Modifier.padding(bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = result.name, style = MaterialTheme.typography.titleMedium)
      Text(text = result.display(), style = MaterialTheme.typography.bodyLarge)
      if (result.description.isNotBlank()) {
        Text(text = result.description, style = MaterialTheme.typography.bodySmall)
      }
    }
    IconButton(onClick = onEdit) {
      Icon(Icons.Default.Edit, contentDescription = "Editar estudio")
    }
  }
}