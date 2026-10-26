package com.santidev.biotrack.data.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

private data class OcrLine(val text: String, val top: Int, val bottom: Int, val left: Int)

class OcrReader(private val context: Context) {
  
  private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  
  // Filas de la tabla; cada fila es la lista de textos de izquierda a derecha
  suspend fun readRows(uris: List<Uri>): List<List<String>> = uris.flatMap { uri ->
    val result = recognizer.process(InputImage.fromFilePath(context, uri)).await()
    val lines = result.textBlocks
      .flatMap { it.lines }
      .mapNotNull { line ->
        line.boundingBox?.let { OcrLine(line.text, it.top, it.bottom, it.left) }
      }
    groupIntoRows(lines).map { row -> row.map { it.text } }
  }
  
  // Texto plano, solo para la pantalla de prueba
  suspend fun read(uris: List<Uri>): String =
    readRows(uris).joinToString("\n") { it.joinToString("  |  ") }
  
  // Une las líneas que están a la misma altura en una misma fila
  private fun groupIntoRows(lines: List<OcrLine>): List<List<OcrLine>> {
    val rows = mutableListOf<MutableList<OcrLine>>()
    lines.sortedBy { it.top }.forEach { line ->
      val center = (line.top + line.bottom) / 2.0
      val currentRow = rows.lastOrNull()?.takeIf { row ->
        val rowCenter = row.map { (it.top + it.bottom) / 2.0 }.average()
        val rowHeight = row.map { (it.bottom - it.top).toDouble() }.average()
        abs(center - rowCenter) < rowHeight * 0.6
      }
      if (currentRow != null) currentRow.add(line) else rows.add(mutableListOf(line))
    }
    return rows.map { row -> row.sortedBy { it.left } }
  }
}