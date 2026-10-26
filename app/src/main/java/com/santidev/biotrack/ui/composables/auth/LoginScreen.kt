package com.santidev.biotrack.ui.composables.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
  val context = LocalContext.current
  val error by viewModel.error.collectAsStateWithLifecycle()
  
  Column(
    modifier = Modifier.fillMaxSize().padding(8.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("BioTrack", /*style = MaterialTheme.typography.displayMedium*/)
    Spacer(Modifier.height(32.dp))
    Button(
      onClick = { viewModel.signIn(context) },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Iniciar sesión con Google", /*style = MaterialTheme.typography.titleLarge*/)
    }
    error?.let {
      Spacer(Modifier.height(16.dp))
      Text(it, color = MaterialTheme.colorScheme.error)
    }
  }
}