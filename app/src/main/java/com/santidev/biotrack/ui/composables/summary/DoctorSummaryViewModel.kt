package com.santidev.biotrack.ui.composables.summary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.model.ParameterDef
import com.santidev.biotrack.data.model.Study
import com.santidev.biotrack.domain.StudiesRepository
import com.santidev.biotrack.ui.composables.studies.details.MeasurementDetail
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface SummaryState {
  data object Loading : SummaryState
  data object Error : SummaryState
  data class Ready(val items: List<MeasurementDetail>) : SummaryState
}

class DoctorSummaryViewModel(private val repository: StudiesRepository) : ViewModel() {
  
  // Se recalcula cuando cambian los parámetros o cuando se agrega/borra un estudio
  val state: StateFlow<SummaryState> = combine<List<ParameterDef>, List<Study>, SummaryState>(
    repository.observeParameters(),
    repository.examineStudies()
  ) { parameters, _ ->
    val items = coroutineScope {
      parameters.map { parameter ->
        async {
          val latest = repository.latestMeasurements(parameter.id) // las 2 más nuevas
          latest.firstOrNull()?.let { current ->
            MeasurementDetail(parameter, current, latest.getOrNull(1))
          }
        }
      }.awaitAll()
    }
      .filterNotNull()
      .sortedBy { it.parameter?.name?.lowercase().orEmpty() }
    SummaryState.Ready(items)
  }.catch { error ->
    Log.e("DoctorSummaryViewModel", "Error al armar el resumen", error)
    emit(SummaryState.Error)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryState.Loading)
}