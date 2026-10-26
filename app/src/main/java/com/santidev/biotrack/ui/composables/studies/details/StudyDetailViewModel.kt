package com.santidev.biotrack.ui.composables.studies.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santidev.biotrack.data.model.Study
import com.santidev.biotrack.domain.StudiesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface StudyDetailState {
  data object Loading : StudyDetailState
  data object NotFound : StudyDetailState
  data object Error : StudyDetailState
  data class Ready(val study: Study, val items: List<MeasurementDetail>) : StudyDetailState
}

class StudyDetailViewModel(
  private val repository: StudiesRepository,
  private val studyId: String
) : ViewModel() {
  
  val state: StateFlow<StudyDetailState> = flow<StudyDetailState> {
    val study = repository.getStudy(studyId)
    if (study == null) {
      emit(StudyDetailState.NotFound)
      return@flow
    }
    emitAll(
      combine(
        repository.observeStudyMeasurements(studyId),
        repository.observeParameters()
      ) { measurements, parameters ->
        val parametersById = parameters.associateBy { it.id }
        val items = measurements.map { m ->
          MeasurementDetail(
            parameter = parametersById[m.parameterId],
            current = m,
            previous = repository.previousMeasurement(m.parameterId, study.date)
          )
        }.sortedBy { it.parameter?.name?.lowercase().orEmpty() }
        StudyDetailState.Ready(study, items)
      }
    )
  }.catch { error ->
    Log.e("StudyDetailViewModel", "Error al cargar el estudio", error)
    emit(StudyDetailState.Error)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyDetailState.Loading)
}