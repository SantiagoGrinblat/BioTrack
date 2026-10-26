package com.santidev.biotrack.ui.composables.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.santidev.biotrack.data.auth.GoogleAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val authClient: GoogleAuthClient) : ViewModel() {
  
  val user: StateFlow<FirebaseUser?> = authClient.authState
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authClient.currentUser)
  
  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error
  
  fun signIn(activityContext: Context) {
    viewModelScope.launch {
      authClient.signIn(activityContext)
        .onFailure { _error.value = "No se pudo iniciar sesion. Intentalo de nuevo" }
    }
  }
  
  fun signOut() {
    viewModelScope.launch { authClient.signOut() }
  }
}