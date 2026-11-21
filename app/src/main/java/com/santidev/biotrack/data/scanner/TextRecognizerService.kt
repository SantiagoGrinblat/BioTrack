package com.santidev.biotrack.data.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.santidev.biotrack.domain.scanner.OcrLine
import com.santidev.biotrack.domain.scanner.OcrPage
import kotlinx.coroutines.tasks.await

//habla con MLKit y nada mas. No sabe nada de estudios, parametros ni unidades.
class TextRecognizerService(private val context: Context) {
  
  private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  
  //lee una imagen (de la galeria, por ejemplo) y devuelve sus lineas con su posicion
  suspend fun read(uri: Uri): OcrPage {
    //fromFilePath ya corrige la rotacion que trae la foto
    val image = InputImage.fromFilePath(context, uri)
    val result = recognizer.process(image).await()
    
    val lines = result.textBlocks
      .flatMap { block -> block.lines }
      .mapNotNull { line ->
        line.boundingBox?.let { box ->
          OcrLine(line.text, box.left, box.top, box.right, box.bottom)
        }
      }
    return OcrPage(width = image.width, height = image.height, lines = lines)
  }
  
  fun close() = recognizer.close()
}