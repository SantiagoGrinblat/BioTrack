package com.santidev.biotrack.ui.screens.initialScreen

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.LabRepository
import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.model.LabResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class LabListViewModel(private val repository: LabRepository) : ViewModel() {
  
  // null = cargando todavía; lista vacía = no hay laboratorios
  val labs: StateFlow<List<Lab>?> = repository.observeLabs()
    .catch { error ->
      Log.e("LabListViewModel", "Error al leer los laboratorios", error)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
  
  var selectionMode by mutableStateOf(false)
    private set
  var selectedIds by mutableStateOf(emptySet<String>())
    private set
  
  fun startSelection() {
    selectedIds = emptySet()
    selectionMode = true
  }
  
  fun cancelSelection() {
    selectionMode = false
    selectedIds = emptySet()
  }
  
  fun toggle(id: String, selected: Boolean) {
    selectedIds = if (selected) selectedIds + id else selectedIds - id
  }
  
  fun deleteSelected() {
    repository.delete(selectedIds.toList())
    cancelSelection()
  }
  
  // Cambia un estudio de un laboratorio (index = su posición en la lista de estudios)
  fun updateResult(lab: Lab, index: Int, result: LabResult) {
    if (index !in lab.results.indices) return
    val newResults = lab.results.toMutableList().also { it[index] = result }
    repository.updateResults(lab.id, newResults)
  }
}