package com.santidev.biotrack.data.scanner

import android.net.Uri
import android.util.Log
import com.santidev.biotrack.data.catalog.CatalogRepository
import com.santidev.biotrack.domain.scanner.PageParser
import com.santidev.biotrack.domain.scanner.ParameterMatcher
import com.santidev.biotrack.domain.scanner.ScannedRow
import com.santidev.biotrack.domain.scanner.StudyDetector

//lo que se lee de una o mas fotos
data class ScanResult(
  val title: String,
  val rows: List<ScannedRow>
)

//junta las partes OCR -> estudios del informe -> parametros de esos estudios -> filas.
//solo se carga el JSON de los estudios que aparecen en el informe.
class LabScanner(
  private val recognizer: TextRecognizerService,
  private val catalog: CatalogRepository
) {
  
  suspend fun scan(uris: List<Uri>): ScanResult {
    val index = catalog.studies()
    val detector = StudyDetector(index)
    
    val studyNames = mutableListOf<String>()
    val rows = mutableListOf<ScannedRow>()
    
    for (uri in uris) {
      val page = recognizer.read(uri)
      page.lines.forEach { Log.w("BIO_OCR", "${it.left},${it.top},${it.bottom} | ${it.text}") }
      
      //los titulos estan en la columna de la izquierda, se leen de arriba hacia abajo
      val texts = page.lines.sortedBy { it.top + it.bottom }.map { it.text }
      val detected = detector.detect(texts)
      Log.w("BIO_OCR", "lineas: " + texts.filter { it.contains("orina", true) || it.contains("examen", true) })
      studyNames += detected.map { it.study.name }
      
      val parameters = detected.mapNotNull { catalog.study(it.study) }.flatMap { it.parameters }
      Log.w("BIO_OCR", "detectados=${detected.map { it.study.id }} parametros=${parameters.size}")
      rows += PageParser(ParameterMatcher(parameters)).parse(page)
    }
    
    return ScanResult(
      title = studyNames.distinct().joinToString(", "),
      //el mismo estudio repetido en el papel (por ejemplo, plaquetas) se deja solo una vez
      rows = rows.distinctBy { Triple(it.name, it.value, it.unit) }
    )
  }
}