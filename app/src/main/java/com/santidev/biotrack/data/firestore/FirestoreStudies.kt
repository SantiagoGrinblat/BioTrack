package com.santidev.biotrack.data.firestore

import com.google.firebase.firestore.Query
import com.santidev.biotrack.data.model.NewMeasurement
import com.santidev.biotrack.data.model.Study
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class FirestoreStudies(private val user: FirestoreUser) {
  
  fun observe(): Flow<List<Study>> = callbackFlow {
    val listener = user.studies()
      .orderBy("date", Query.Direction.DESCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.toObjects(Study::class.java).orEmpty())
      }
    awaitClose { listener.remove() }
  }
  
  suspend fun get(studyId: String): Study? =
    user.studies().document(studyId)
      .get()
      .await()
      .toObject(Study::class.java)
  
  suspend fun save(
    title: String,
    date: Timestamp,
    measurements: List<NewMeasurement>
  ) {
    val studyRef = user.studies().document()
    val batch = user.db.batch()
    
    batch.set(studyRef, mapOf("title" to title, "date" to date))
    measurements.forEach { m ->
      val measurementRef = user.measurements().document()
      batch.set(
        measurementRef,
        mapOf(
          "studyId" to studyRef.id,
          "parameterId" to m.parameterId,
          "valueText" to m.valueText,
          "value" to m.value,
          "date" to date // copiada del estudio
        )
      )
    }
    batch.commit() // sin await: así no se cuelga si no hay internet
  }
  
  suspend fun delete(studyIds: List<String>) {
    studyIds.forEach { studyId ->
      val measurements = user.measurements()
        .whereEqualTo("studyId", studyId)
        .get()
        .await()
      
      // El estudio va al final, para que sea lo último que se borra
      val refs = measurements.documents.map { it.reference } +
        user.studies().document(studyId)
      
      // Un batch admite hasta 500 operaciones
      refs.chunked(450).forEach { chunk ->
        val batch = user.db.batch()
        chunk.forEach { batch.delete(it) }
        batch.commit() // sin await: así no se cuelga si no hay internet
      }
    }
  }
}