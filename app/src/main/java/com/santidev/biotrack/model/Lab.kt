package com.santidev.biotrack.model

import com.google.firebase.firestore.DocumentId

data class Lab(
  @DocumentId val id: String = "",
  val title: String = "",
  val date: Long = 0L,
  val results: List<LabResult> = emptyList()
)

data class LabResult(
  val name: String = "",
  val value: String = "",
  val unit: String = "",
  val minutes: Int? = null,
  val seconds: Int? = null
)

fun LabResult.display(): String = listOfNotNull(
  value.takeIf { it.isNotBlank() },
  unit.takeIf { it.isNotBlank() },
  minutes?.let { "$it min" },
  seconds?.let { "$it seg" }
).joinToString(" ")