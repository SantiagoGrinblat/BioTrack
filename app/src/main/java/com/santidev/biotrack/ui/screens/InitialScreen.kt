package com.santidev.biotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.santidev.biotrack.navigation.Routes

@Composable
fun InitialScreen(onNavigate: (Routes) -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("Inicio pantalla")
    Button(
      onClick = {
        onNavigate(Routes.Info)
      }
    ) {
      Text("ir a la info")
    }
  }
}