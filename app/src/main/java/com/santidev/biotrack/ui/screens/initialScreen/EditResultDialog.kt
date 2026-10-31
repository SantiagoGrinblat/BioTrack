package com.santidev.biotrack.ui.screens.initialScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.model.LabResult
import com.santidev.biotrack.utils.ResultFields
import com.santidev.biotrack.utils.toForm
import com.santidev.biotrack.utils.toLabResult

// Edita un solo estudio: nombre, resultado, unidad y tiempo
@Composable
fun EditResultDialog(
  result: LabResult,
  onSave: (LabResult) -> Unit,
  onDismiss: () -> Unit
) {
  var form by remember { mutableStateOf(result.toForm()) }
  var error by remember { mutableStateOf<String?>(null) }
  
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Editar estudio") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ResultFields(form) { form = it }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        if (form.name.isBlank() || !form.hasResult) {
          error = "Revisá que el estudio tenga nombre y resultado."
        } else {
          onSave(form.toLabResult())
        }
      }) { Text("Guardar") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    }
  )
}