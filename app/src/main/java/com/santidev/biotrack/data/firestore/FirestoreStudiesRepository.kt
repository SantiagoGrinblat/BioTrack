package com.santidev.biotrack.data.firestore

import com.google.firebase.Timestamp
import com.santidev.biotrack.data.model.NewMeasurement
import com.santidev.biotrack.domain.StudiesRepository

class FirestoreStudiesRepository(
  user: FirestoreUser = FirestoreUser(),
  private val parametersDb: FirestoreParameters = FirestoreParameters(user),
  private val studiesDb: FirestoreStudies = FirestoreStudies(user),
  private val measurementsDb: FirestoreMeasurements = FirestoreMeasurements(user)
) : StudiesRepository {
  
  override fun observeParameters() = parametersDb.observe()
  
  override suspend fun createParameter(
    name: String, unit: String, rangeMin: Double?, rangeMax: Double?
  ) = parametersDb.create(name, unit, rangeMin, rangeMax)
  
  override fun examineStudies() = studiesDb.observe()
  
  override suspend fun getStudy(studyId: String) = studiesDb.get(studyId)
  
  override suspend fun saveStudy(
    title: String, date: Timestamp, measurements: List<NewMeasurement>
  ) = studiesDb.save(title, date, measurements)
  
  override suspend fun deleteStudies(studyIds: List<String>) = studiesDb.delete(studyIds)
  
  override fun observeStudyMeasurements(studyId: String) = measurementsDb.observeByStudy(studyId)
  
  override suspend fun latestMeasurements(parameterId: String) = measurementsDb.latest(parameterId)
  
  override suspend fun previousMeasurement(parameterId: String, before: Timestamp) =
    measurementsDb.previous(parameterId, before)
}