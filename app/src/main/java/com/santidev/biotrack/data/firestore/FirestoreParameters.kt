package com.santidev.biotrack.data.firestore

import com.santidev.biotrack.data.model.ParameterDef
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.collections.orEmpty
import kotlin.text.set

class FirestoreParameters(private val user: FirestoreUser) {
  
  fun observe(): Flow<List<ParameterDef>> = callbackFlow {
    val listener = user.parameters()
      .orderBy("name")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.toObjects(ParameterDef::class.java).orEmpty())
      }
    awaitClose { listener.remove() }
  }
  
  suspend fun create(
    name: String,
    unit: String,
    rangeMin: Double?,
    rangeMax: Double?
  ): String {
    val ref = user.parameters().document()
    ref.set(
      mapOf(
        "name" to name,
        "unit" to unit,
        "rangeMin" to rangeMin,
        "rangeMax" to rangeMax
      )
    ) // sin await: así no se cuelga si no hay internet
    return ref.id
  }
  
}