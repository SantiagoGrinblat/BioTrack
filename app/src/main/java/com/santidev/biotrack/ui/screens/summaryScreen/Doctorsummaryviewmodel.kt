package com.santidev.biotrack.ui.screens.summaryScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.LabRepository
import com.santidev.biotrack.domain.SummaryFolder
import com.santidev.biotrack.domain.buildSummary
import com.santidev.biotrack.domain.groupByFolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DoctorSummaryViewModel(repository: LabRepository) : ViewModel() {
  
  //null = la lista esta cargando todavia,
  //lista vacia = no hay datos en esa lista
  val items: StateFlow<List<SummaryFolder>?> = repository.observeLabs()
    .map { labs -> groupByFolder(buildSummary(labs)) }
    .catch { error ->
      Log.e("DoctorSummaryViewModel", "Error al armar el resumen", error)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}