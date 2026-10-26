package com.santidev.biotrack.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.santidev.biotrack.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthClient(private val appContext: Context) {
  
  private val credentialManager = CredentialManager.create(appContext)
  private val auth = FirebaseAuth.getInstance()
  
  val currentUser: FirebaseUser? get() = auth.currentUser
  
  // Emite el usuario actual y cada vez que cambia (login / logout)
  val authState: Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }
  
  // activityContext tiene que ser una Activity
  suspend fun signIn(activityContext: Context): Result<FirebaseUser> {
    return try {
      val option = GetSignInWithGoogleOption.Builder(
        appContext.getString(R.string.web_client)
      ).build()
      val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()
      
      val credential = credentialManager.getCredential(activityContext, request).credential
      if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      ) {
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(firebaseCredential).await().user
        if (user != null) Result.success(user)
        else Result.failure(IllegalStateException("Usuario nulo"))
      } else {
        Result.failure(IllegalStateException("Credencial no soportada"))
      }
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      Result.failure(exception)
    }
  }
  
  suspend fun signOut() {
    auth.signOut()
    credentialManager.clearCredentialState(ClearCredentialStateRequest())
  }
}