package com.santidev.biotrack.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.model.LabResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Guarda en: users/{uid}/labs/{id}
class LabRepository(
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
  
  // El uid se lee en cada llamada, así sigue andando si cambia la cuenta
  private fun labs() = db.collection("users")
    .document(requireNotNull(auth.currentUser?.uid) { "No hay sesión iniciada" })
    .collection("labs")
  
  fun observeLabs(): Flow<List<Lab>> = callbackFlow {
    val listener = labs()
      .orderBy("date", Query.Direction.DESCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.toObjects(Lab::class.java).orEmpty())
      }
    awaitClose { listener.remove() }
  }
  
  // Sin esperar la respuesta: así no se cuelga si no hay internet.
  // Firestore lo sube solo cuando vuelve la conexión.
  fun save(title: String, date: Long, results: List<LabResult>) {
    val data = mapOf(
      "title" to title,
      "date" to date,
      "results" to results.map { it.toMap() }
    )
    labs().document().set(data)
      .addOnFailureListener { Log.e("LabRepository", "No se pudo guardar el laboratorio", it) }
  }
  
  // Sin esperar la respuesta (igual que save): así no se cuelga si no hay internet
  fun delete(labIds: List<String>) {
    labIds.chunked(450).forEach { chunk ->   // un batch admite hasta 500 operaciones
      val batch = db.batch()
      chunk.forEach { id -> batch.delete(labs().document(id)) }
      batch.commit()
        .addOnFailureListener { Log.e("LabRepository", "No se pudo borrar", it) }
    }
  }
  
  // Un estudio como lo guarda Firestore
  private fun LabResult.toMap() = mapOf(
    "name" to name,
    "value" to value,
    "unit" to unit,
    "minutes" to minutes,
    "seconds" to seconds
  )
  
  // Cambia solo los estudios de un laboratorio (título y fecha no se tocan)
  fun updateResults(labId: String, results: List<LabResult>) {
    labs().document(labId).update("results", results.map { it.toMap() })
      .addOnFailureListener { Log.e("LabRepository", "No se pudo actualizar el laboratorio", it) }
  }
}