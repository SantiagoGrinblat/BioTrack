package com.santidev.biotrack.data.model

import com.google.firebase.firestore.DocumentId

data class ParameterDef(
  @DocumentId val id: String = "",
  val name: String = "",
  val unit: String = "",
  val rangeMin: Double? = null,
  val rangeMax: Double? = null
)