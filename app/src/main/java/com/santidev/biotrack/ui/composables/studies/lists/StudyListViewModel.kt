package com.santidev.biotrack.ui.composables.studies.lists

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.model.Study
import com.santidev.biotrack.domain.StudiesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyListViewModel(private val repository: StudiesRepository) : ViewModel() {
  
  // null = cargando todavía; lista vacía = no hay estudios
  val studies: StateFlow<List<Study>?> = repository.examineStudies()
    .catch { error ->
      Log.e("StudyListViewModel", "Error al leer estudios", error)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
  
  var selectionMode by mutableStateOf(false)
    private set
  var selectedIds by mutableStateOf(emptySet<String>())
    private set
  var deleting by mutableStateOf(false)
    private set
  
  fun startSelection() {
    selectedIds = emptySet()
    selectionMode = true
  }
  
  fun cancelSelection() {
    selectionMode = false
    selectedIds = emptySet()
  }
  
  fun toggle(studyId: String) {
    selectedIds = if (studyId in selectedIds) selectedIds - studyId else selectedIds + studyId
  }
  
  fun deleteSelected() {
    if (deleting || selectedIds.isEmpty()) return
    deleting = true
    viewModelScope.launch {
      try {
        repository.deleteStudies(selectedIds.toList())
      } catch (e: Exception) {
        Log.e("StudyListViewModel", "Error al borrar estudios", e)
      } finally {
        deleting = false
        cancelSelection()
      }
    }
  }
}