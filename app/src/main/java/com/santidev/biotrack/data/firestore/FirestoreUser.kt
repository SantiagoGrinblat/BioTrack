package com.santidev.biotrack.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreUser(
  val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
  // Se lee el uid en cada llamada, así sigue andando si cambia la cuenta
  private fun doc() = db.collection("users")
    .document(requireNotNull(auth.currentUser?.uid) { "No hay sesión iniciada" })
  
  fun parameters() = doc().collection("parameters")
  fun studies() = doc().collection("studies")
  fun measurements() = doc().collection("measurements")
}