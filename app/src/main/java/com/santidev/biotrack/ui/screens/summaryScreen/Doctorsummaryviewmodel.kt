package com.santidev.biotrack.ui.screens.summaryScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.LabRepository
import com.santidev.biotrack.domain.SummaryItem
import com.santidev.biotrack.domain.buildSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.emptyList

class DoctorSummaryViewModel(repository: LabRepository) : ViewModel() {
  
  // null = cargando todavía; lista vacía = no hay datos
  val items: StateFlow<List<SummaryItem>?> = repository.observeLabs()
    .map { labs -> buildSummary(labs) }
    .catch { error ->
      Log.e("DoctorSummaryViewModel", "Error al armar el resumen", error)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}