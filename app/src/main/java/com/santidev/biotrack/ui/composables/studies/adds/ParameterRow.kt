package com.santidev.biotrack.ui.composables.studies.adds

import java.util.UUID

data class ParameterRow(
  val key: String = UUID.randomUUID().toString(),
  val name: String = "",
  val unit: String = "",
  val value: String = "",
  val rangeMin: Double? = null,
  val rangeMax: Double? = null,
  val needsReview: Boolean = false
)