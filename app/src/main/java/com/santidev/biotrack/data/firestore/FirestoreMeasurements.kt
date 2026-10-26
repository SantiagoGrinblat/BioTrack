package com.santidev.biotrack.data.firestore

import com.google.firebase.firestore.Query
import com.santidev.biotrack.data.model.Measurement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class FirestoreMeasurements(private val user: FirestoreUser) {
  
  fun observeByStudy(studyId: String): Flow<List<Measurement>> = callbackFlow {
    val listener = user.measurements()
      .whereEqualTo("studyId", studyId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.toObjects(Measurement::class.java).orEmpty())
      }
    awaitClose { listener.remove() }
  }
  
  suspend fun latest(parameterId: String): List<Measurement> =
    user.measurements()
      .whereEqualTo("parameterId", parameterId)
      .orderBy("date", Query.Direction.DESCENDING)
      .limit(2)
      .get()
      .await()
      .toObjects(Measurement::class.java)
  
  suspend fun previous(parameterId: String, before: Timestamp): Measurement? =
    user.measurements()
      .whereEqualTo("parameterId", parameterId)
      .whereLessThan("date", before)
      .orderBy("date", Query.Direction.DESCENDING)
      .limit(1)
      .get()
      .await()
      .toObjects(Measurement::class.java)
      .firstOrNull()
}