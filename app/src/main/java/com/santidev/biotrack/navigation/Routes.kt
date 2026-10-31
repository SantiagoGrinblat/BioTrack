package com.santidev.biotrack.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Routes : NavKey {
  
  @Serializable
  data object LabList : Routes()
  
  @Serializable
  data object NewLab : Routes()
  
  @Serializable
  data object DoctorSummary : Routes()
}