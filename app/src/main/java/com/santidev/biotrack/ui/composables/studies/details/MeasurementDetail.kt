package com.santidev.biotrack.ui.composables.studies.details

import com.santidev.biotrack.data.model.Measurement
import com.santidev.biotrack.data.model.ParameterDef

data class MeasurementDetail(
  val parameter: ParameterDef?,
  val current: Measurement,
  val previous: Measurement?
) {
  val difference: Double?
    get() {
      val c = current.value ?: return null
      val p = previous?.value ?: return null
      return c - p
    }
  
  val outOfRange: Boolean
    get() {
      val p = parameter ?: return false
      val v = current.value ?: return false
      return p.rangeMin?.let { v < it } == true ||
        p.rangeMax?.let { v > it } == true
    }
}