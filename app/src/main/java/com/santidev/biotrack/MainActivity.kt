package com.santidev.biotrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.santidev.biotrack.ui.composables.auth.AuthViewModel
import com.santidev.biotrack.data.auth.GoogleAuthClient
import com.santidev.biotrack.navigation.NavWrapper
import com.santidev.biotrack.ui.theme.BioTrackTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val authClient = GoogleAuthClient(applicationContext)
    val authViewModel = ViewModelProvider(this, viewModelFactory {
      initializer { AuthViewModel(authClient) }
    })[AuthViewModel::class.java]
    
    setContent {
      BioTrackTheme {
        NavWrapper(authViewModel)
      }
    }
  }
}