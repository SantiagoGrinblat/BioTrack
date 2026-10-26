package com.santidev.biotrack.ui.composables.studies.adds

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.santidev.biotrack.data.model.NewMeasurement
import com.santidev.biotrack.data.model.ParameterDef
import com.santidev.biotrack.domain.StudiesRepository
import com.santidev.biotrack.domain.parseNumber
import com.santidev.biotrack.domain.scanner.parseStudy
import com.santidev.biotrack.ui.common.todayAtNoon
import com.santidev.biotrack.ui.common.utcMidnightToLocalNoon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class AddStudyViewModel(private val repository: StudiesRepository) : ViewModel() {
  
  var title by mutableStateOf("")
    private set
  var dateMillis by mutableLongStateOf(todayAtNoon())
    private set
  var rows by mutableStateOf(listOf(ParameterRow()))
    private set
  var error by mutableStateOf<String?>(null)
    private set
  var saving by mutableStateOf(false)
    private set
  var fromScan by mutableStateOf(false)
    private set
  
  // Parámetros ya existentes, para autocompletar
  val knownParameters: StateFlow<List<ParameterDef>> = repository.observeParameters()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  
  fun onTitleChange(value: String) {
    title = value
  }
  
  fun onDateSelected(utcMillis: Long) {
    dateMillis = utcMidnightToLocalNoon(utcMillis)
  }
  
  fun addRow() {
    rows = rows + ParameterRow()
  }
  
  fun removeRow(key: String) {
    rows = rows.filterNot { it.key == key }.ifEmpty { listOf(ParameterRow()) }
  }
  
  fun onNameChange(key: String, name: String) {
    val known = findKnown(name)
    updateRow(key) { it.copy(name = name, unit = known?.unit ?: it.unit) }
  }
  
  fun onUnitChange(key: String, unit: String) = updateRow(key) { it.copy(unit = unit) }
  
  fun onValueChange(key: String, value: String) = updateRow(key) { it.copy(value = value) }
  
  fun pickSuggestion(key: String, parameter: ParameterDef) =
    updateRow(key) { it.copy(name = parameter.name, unit = parameter.unit) }
  
  fun findKnown(name: String): ParameterDef? =
    knownParameters.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

//  private fun updateRow(key: String, transform: (ParameterRow) -> ParameterRow) {
//    rows = rows.map { if (it.key == key) transform(it) else it }
//  }
  
  private fun updateRow(key: String, transform: (ParameterRow) -> ParameterRow) {
    rows = rows.map { if (it.key == key) transform(it).copy(needsReview = false) else it }
  }
  
  fun save(onDone: () -> Unit) {
    if (saving) return
    
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      error = "Escribí el título del estudio."
      return
    }
    
    val filled = rows.filter { it.name.isNotBlank() || it.value.isNotBlank() }
    if (filled.isEmpty()) {
      error = "Agregá al menos un parámetro con su valor."
      return
    }
    
    // La coma decimal es la habitual en Argentina: la aceptamos
    // Cualquier texto es válido como valor; solo se exige que no esté vacío
    val parsed = filled.map { row ->
      val text = row.value.trim()
      if (row.name.isBlank() || text.isEmpty()) {
        error = "Revisá que cada parámetro tenga nombre y valor."
        return
      }
      row to text
    }
    
    if (parsed.map { it.first.name.trim().lowercase() }.distinct().size != parsed.size) {
      error = "Hay un parámetro repetido en este estudio."
      return
    }
    
    saving = true
    error = null
    viewModelScope.launch {
      try {
        val measurements = parsed.map { (row, text) ->
          val name = row.name.trim()
          val parameterId = findKnown(name)?.id
            ?: repository.createParameter(name, row.unit.trim(), row.rangeMin, row.rangeMax)
          NewMeasurement(parameterId, text, parseNumber(text))
        }
        repository.saveStudy(cleanTitle, Timestamp(Date(dateMillis)), measurements)
        saving = false
        onDone()
      } catch (e: Exception) {
        Log.e("AddStudyViewModel", "Error al guardar el estudio", e)
        error = "No se pudo guardar. Intentá de nuevo."
        saving = false
      }
    }
  }
  
  fun onScanned(ocrRows: List<List<String>>) {
    val parsed = parseStudy(ocrRows, knownParameters.value.map { it.name })
    if (parsed.rows.isEmpty()) {
      error = "No se encontraron parámetros en la foto. Probá con más luz o cargalos a mano."
      return
    }
    if (title.isBlank()) parsed.titleSuggestion?.let { title = it }
    
    val scanned = parsed.rows.map { p ->
      val known = findKnown(p.name) // si el parámetro ya existe, se reutiliza con su unidad
      ParameterRow(
        name = known?.name ?: p.name,
        unit = known?.unit ?: p.unit,
        value = p.valueText,
        rangeMin = p.rangeMin,
        rangeMax = p.rangeMax,
        needsReview = p.needsReview
      )
    }
    // Se conservan las filas que ya hubieran sido cargadas a mano
    rows = rows.filter { it.name.isNotBlank() || it.value.isNotBlank() } + scanned
    fromScan = true
    error = null
  }
  
  fun onScanError(message: String) {
    error = message
  }
}