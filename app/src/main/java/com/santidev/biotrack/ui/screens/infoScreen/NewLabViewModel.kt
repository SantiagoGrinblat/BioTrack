package com.santidev.biotrack.ui.screens.infoScreen

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.LabRepository
import com.santidev.biotrack.data.scanner.LabScanner
import com.santidev.biotrack.domain.scanner.ScannedRow
import com.santidev.biotrack.utils.ResultForm
import com.santidev.biotrack.utils.toLabResult
import com.santidev.biotrack.ui.todayUtcMillis
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class NewLabViewModel(
  private val repository: LabRepository,
  private val scanner: LabScanner
) : ViewModel() {
  
  var title by mutableStateOf("")
    private set
  var date by mutableLongStateOf(todayUtcMillis())
    private set
  var results by mutableStateOf(listOf(ResultForm()))
    private set
  var error by mutableStateOf<String?>(null)
    private set
  var scanning by mutableStateOf(false)
    private set
  
  fun onTitleChange(value: String) {
    title = value
  }
  
  fun onDateChange(utcMillis: Long) {
    date = utcMillis
  }
  
  fun addResult() {
    results = results + ResultForm()
  }
  
  fun removeResult(id: String) {
    results = results.filterNot { it.id == id }.ifEmpty { listOf(ResultForm()) }
  }
  
  // Cambia una fila: viewModel.change(row.id) { it.copy(name = "Hematocrito") }
  fun change(id: String, transform: (ResultForm) -> ResultForm) {
    results = results.map { if (it.id == id) transform(it).copy(needsReview = false) else it }
  }
  
  fun save(onDone: () -> Unit) {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      error = "Escribí el título del laboratorio."
      return
    }
    
    val filled = results.filterNot { it.isEmpty }
    if (filled.isEmpty()) {
      error = "Agregá al menos un estudio."
      return
    }
    if (filled.any { it.name.isBlank() || !it.hasResult }) {
      error = "Revisá que cada estudio tenga nombre y resultado."
      return
    }
    
    error = null
    repository.save(cleanTitle, date, filled.map { it.toLabResult() })
    onDone()
  }
  
  fun scan(uris: List<Uri>) {
    if (scanning) return
    scanning = true
    error = null
    viewModelScope.launch {
      try {
        val result = scanner.scan(uris)
        if (result.rows.isEmpty()) {
          error = "No se encontraron estudios en la foto. Probá con más luz o cargalos a mano."
        } else {
          if (title.isBlank()) title = result.title
          // Se conservan las filas que ya se hubieran cargado a mano
          results = results.filterNot { it.isEmpty } + result.rows.map { it.toForm() }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("NewLabViewModel", "Error al leer el informe", e)
        error = "No se pudo leer el informe. Intentá de nuevo."
      } finally {
        scanning = false
      }
    }
  }
  
  fun onScanError(message: String) {
    error = message
  }
  
  private fun ScannedRow.toForm() = ResultForm(
    name = name, value = value, unit = unit,
    description = description, needsReview = needsReview
  )
}