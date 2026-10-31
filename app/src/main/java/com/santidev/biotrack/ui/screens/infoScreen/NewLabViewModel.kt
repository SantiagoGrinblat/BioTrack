package com.santidev.biotrack.ui.screens.infoScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.santidev.biotrack.data.LabRepository
import com.santidev.biotrack.utils.ResultForm
import com.santidev.biotrack.utils.toLabResult
import com.santidev.biotrack.ui.todayUtcMillis

class NewLabViewModel(private val repository: LabRepository) : ViewModel() {
  
  var title by mutableStateOf("")
    private set
  var date by mutableLongStateOf(todayUtcMillis())
    private set
  var results by mutableStateOf(listOf(ResultForm()))
    private set
  var error by mutableStateOf<String?>(null)
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
    results = results.map { if (it.id == id) transform(it) else it }
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
}