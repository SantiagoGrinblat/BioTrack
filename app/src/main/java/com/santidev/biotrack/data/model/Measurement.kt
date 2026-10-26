package com.santidev.biotrack.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Measurement(
  @DocumentId val id: String = "",
  val studyId: String = "",
  val parameterId: String = "",
  val valueText: String = "",
  val value: Double? = null,
  val date: Timestamp = Timestamp.now()
)

// Lo que carga el usuario al armar un estudio (todavía sin id ni fecha)
data class NewMeasurement(
  val parameterId: String,
  val valueText: String,
  val value: Double?
)