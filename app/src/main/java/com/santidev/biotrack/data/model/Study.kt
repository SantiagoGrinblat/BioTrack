package com.santidev.biotrack.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Study(
  @DocumentId val id: String = "",
  val title: String = "",
  val date: Timestamp = Timestamp.now()
)