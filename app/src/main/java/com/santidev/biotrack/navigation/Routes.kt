package com.santidev.biotrack.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Routes: NavKey {
  
  @Serializable
  data object Info: Routes()
  
  @Serializable
  data object StudyList: Routes()
  
  @Serializable
  data object AddStudy: Routes()
  
  @Serializable
  data class StudyDetail(val studyId: String)
  
  @Serializable
  data object DoctorSummary: Routes()
}