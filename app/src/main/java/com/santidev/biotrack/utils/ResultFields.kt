package com.santidev.biotrack.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.model.LabResult
import java.util.UUID

//al guardar, los minutos y segundos se pasan a numero entero.
data class ResultForm(
  val id: String = UUID.randomUUID().toString(),
  val name: String = "",
  val folder: String = "",
  val value: String = "",
  val unit: String = "",
  val useMinutes: Boolean = false,
  val minutes: String = "",
  val useSeconds: Boolean = false,
  val seconds: String = "",
  val description: String = "",
  val needsReview: Boolean = false,
) {
  val hasResult: Boolean
    get() = value.isNotBlank() ||
      (useMinutes && minutes.isNotBlank()) ||
      (useSeconds && seconds.isNotBlank())
  
  val isEmpty: Boolean
    get() = name.isBlank() && unit.isBlank() && description.isBlank() && !hasResult
}

//de un estudio guardado se pasa al formulario (para editarlo)
fun LabResult.toForm() = ResultForm(
  name = name,
  folder = folder,
  value = value,
  unit = unit,
  useMinutes = minutes != null,
  minutes = minutes?.toString().orEmpty(),
  useSeconds = seconds != null,
  seconds = seconds?.toString().orEmpty(),
  description = description,
)

//del formulario se pasa al estudio que se guarda
fun ResultForm.toLabResult() = LabResult(
  name = name.trim(),
  folder = folder.trim(),
  value = value.trim(),
  unit = unit.trim(),
  minutes = if (useMinutes) minutes.toIntOrNull() else null,
  seconds = if (useSeconds) seconds.toIntOrNull() else null,
  description = description,
)

//los campos de un estudio son = nombre, carpeta a la pertenece, resultado, unidad, metodo de estudio, descripcion y si es un tiempo, minutos y segundos.
//los usan el formulario de nuevo laboratorio y el dialogo de editar.
@Composable
fun ResultFields(row: ResultForm, onChange: (ResultForm) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(
      value = row.name,
      onValueChange = { text -> onChange(row.copy(name = text)) },
      label = { Text("Estudio (ej. Hematocrito)") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      modifier = Modifier.fillMaxWidth()
    )
    
    OutlinedTextField(
      value = row.folder,
      onValueChange = { text -> onChange(row.copy(folder = text)) },
      label = { Text("Carpeta (ej. Hepatograma)") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      modifier = Modifier.fillMaxWidth()
    )
    
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = row.value,
        onValueChange = { text -> onChange(row.copy(value = text)) },
        label = { Text("Resultado") },
        singleLine = true,
        modifier = Modifier.weight(1f)
      )
      OutlinedTextField(
        value = row.unit,
        onValueChange = { text -> onChange(row.copy(unit = text)) },
        label = { Text("Unidad") },
        singleLine = true,
        modifier = Modifier.weight(1f)
      )
    }
    
    //si el resultado es un tiempo = se elige minutos y/o segundos y aparece su campo
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = row.useMinutes,
        onClick = { onChange(row.copy(useMinutes = !row.useMinutes)) },
        label = { Text("Minutos") }
      )
      FilterChip(
        selected = row.useSeconds,
        onClick = { onChange(row.copy(useSeconds = !row.useSeconds)) },
        label = { Text("Segundos") }
      )
    }
    
    if (row.useMinutes) {
      OutlinedTextField(
        value = row.minutes,
        onValueChange = { text ->
          onChange(
            row.copy(
              minutes = text.filter(Char::isDigit).take(4)
            )
          )
        },
        label = { Text("Minutos (número entero)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
      )
    }
    if (row.useSeconds) {
      OutlinedTextField(
        value = row.seconds,
        onValueChange = { text ->
          onChange(
            row.copy(
              seconds = text.filter(Char::isDigit).take(4)
            )
          )
        },
        label = { Text("Segundos (número entero)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
      )
    }
    OutlinedTextField(
      value = row.description,
      onValueChange = { text -> onChange(row.copy(description = text)) },
      label = { Text("Descripción del estudio (opcional)") },
      minLines = 1,
      maxLines = 3,
      modifier = Modifier.fillMaxWidth()
    )
  }
}