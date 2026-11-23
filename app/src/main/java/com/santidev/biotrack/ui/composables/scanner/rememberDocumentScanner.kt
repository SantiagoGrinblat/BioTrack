package com.santidev.biotrack.ui.composables.scanner

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

//abre el escaner de documentos de Google (recorta, corrige la perspectiva y permite
//elegir fotos de la galeria) y devuelve las imáaenes.
//la funcion que devuelve abre el escaner.
@Composable
fun rememberDocumentScanner(
  onResult: (List<Uri>) -> Unit,
  onError: (String) -> Unit
): () -> Unit {
  val activity = LocalActivity.current
  val currentOnResult by rememberUpdatedState(onResult)
  val currentOnError by rememberUpdatedState(onError)
  
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val uris = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        ?.pages?.map { it.imageUri }.orEmpty()
      if (uris.isNotEmpty()) currentOnResult(uris)
    }
  }
  
  return {
    val currentActivity = activity
    if (currentActivity == null) {
      currentOnError("No se pudo abrir el escáner.")
    } else {
      val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(4)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()
      GmsDocumentScanning.getClient(options)
        .getStartScanIntent(currentActivity)
        .addOnSuccessListener { launcher.launch(IntentSenderRequest.Builder(it).build()) }
        .addOnFailureListener { currentOnError("No se pudo abrir el escáner.") }
    }
  }
}