package com.santidev.biotrack.ui.composables.scanner

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.santidev.biotrack.data.scanner.OcrReader
import kotlinx.coroutines.launch

@Composable
fun rememberStudyScanner(
  onResult: (List<List<String>>) -> Unit,
  onError: (String) -> Unit
): () -> Unit {
  val context = LocalContext.current
  val activity = LocalActivity.current
  val scope = rememberCoroutineScope()
  val currentOnResult by rememberUpdatedState(onResult)
  val currentOnError by rememberUpdatedState(onError)
  
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val uris = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        ?.pages?.map { it.imageUri }.orEmpty()
      scope.launch {
        runCatching { OcrReader(context).readRows(uris) }
          .onSuccess { currentOnResult(it) }
          .onFailure { currentOnError("No se pudo leer el estudio. Intentá de nuevo.") }
      }
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