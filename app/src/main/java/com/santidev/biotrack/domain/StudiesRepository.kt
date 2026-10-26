package com.santidev.biotrack.domain

import com.google.firebase.Timestamp
import com.santidev.biotrack.data.model.Measurement
import com.santidev.biotrack.data.model.NewMeasurement
import com.santidev.biotrack.data.model.ParameterDef
import com.santidev.biotrack.data.model.Study
import kotlinx.coroutines.flow.Flow

interface StudiesRepository {
  
  fun observeParameters(): Flow<List<ParameterDef>>
  fun examineStudies(): Flow<List<Study>>
  fun observeStudyMeasurements(studyId: String): Flow<List<Measurement>>
  
  suspend fun createParameter(
    name: String,
    unit: String,
    rangeMin: Double? = null,
    rangeMax: Double? = null
  ): String // devuelve el id del parámetro creado
  
  // Estudio + todas sus mediciones en un solo batch
  suspend fun saveStudy(
    title: String,
    date: Timestamp,
    measurements: List<NewMeasurement>
  )
  
  // Las 2 más recientes, de la más nueva a la más vieja
  suspend fun latestMeasurements(parameterId: String): List<Measurement>
  
  suspend fun getStudy(studyId: String): Study?
  
  // La medición inmediatamente anterior a una fecha (null si es la primera)
  suspend fun previousMeasurement(parameterId: String, before: Timestamp): Measurement?
  
  suspend fun deleteStudies(studyIds: List<String>)
}