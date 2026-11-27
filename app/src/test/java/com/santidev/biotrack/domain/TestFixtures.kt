package com.santidev.biotrack.domain

import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.model.LabResult

internal fun labResult(
  name: String = "Glucemia",
  value: String = "",
  unit: String = "mg/dL",
  minutes: Int? = null,
  seconds: Int? = null,
  folder: String = ""
) = LabResult(
  name = name, folder = folder, value = value, unit = unit,
  minutes = minutes, seconds = seconds
)

internal fun lab(date: Long, vararg results: LabResult) =
  Lab(date = date, results = results.toList())