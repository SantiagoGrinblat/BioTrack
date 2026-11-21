package com.santidev.biotrack.domain.scanner

import com.santidev.biotrack.domain.catalog.StudyEntry
import java.text.Normalizer.normalize

// Qué tan segura es la coincidencia. Sirve para marcar lo dudoso en la revisión.
enum class Confidence { EXACT, PREFIX, FUZZY }

data class StudyMatch(
  val study: StudyEntry,
  val lineIndex: Int,   // en qué línea del informe apareció su título
  val text: String,     // esa línea, tal como se leyó
  val confidence: Confidence
)

// Encuentra los títulos de estudios (HEMOGRAMA, FERREMIA...) entre las líneas de un informe.
// Un informe puede traer varios estudios: se devuelve uno por estudio, en el orden en que aparecen.
class StudyDetector(studies: List<StudyEntry>) {
  
  private class Key(val study: StudyEntry, val normalized: String, val compact: String)
  
  private val keys: List<Key> = studies
    .flatMap { study ->
      (study.aliases + study.name).distinct().map { Key(study, normalize(it), compact(it)) }
    }
    .filter { it.compact.isNotEmpty() }
  
  // lines = el texto de cada línea del informe, de arriba hacia abajo
  fun detect(lines: List<String>): List<StudyMatch> {
    val found = LinkedHashMap<String, StudyMatch>()
    lines.forEachIndexed { index, text ->
      val (study, confidence) = bestMatch(text) ?: return@forEachIndexed
      // Si el título aparece más de una vez, vale la primera
      if (study.id !in found) found[study.id] = StudyMatch(study, index, text, confidence)
    }
    return found.values.sortedBy { it.lineIndex }
  }
  
  // Se elige la coincidencia más segura y, si hay empate, la del nombre más largo
  // (así "glucemia post prandial" gana sobre "glucemia")
  private fun bestMatch(text: String): Pair<StudyEntry, Confidence>? {
    val line = normalize(text)
    if (line.isEmpty()) return null
    val lineCompact = line.replace(" ", "")
    
    var best: Pair<Key, Confidence>? = null
    for (key in keys) {
      val confidence = confidenceOf(line, lineCompact, key) ?: continue
      val current = best
      if (current == null ||
        confidence < current.second ||
        (confidence == current.second && key.compact.length > current.first.compact.length)
      ) {
        best = key to confidence
      }
    }
    return best?.let { (key, confidence) -> key.study to confidence }
  }
  
  private fun confidenceOf(line: String, lineCompact: String, key: Key): Confidence? {
    // La línea es el nombre: "HEMOGRAMA"
    if (line == key.normalized || lineCompact == key.compact) return Confidence.EXACT
    // La línea empieza con el nombre: "HEMOGRAMA (AA)"
    if (line.startsWith(key.normalized + " ")) return Confidence.PREFIX
    // Lo mismo, con una palabra partida por el OCR: "HEPA TOGRAMA (AA)"
    if (key.compact.length >= MIN_COMPACT_PREFIX && lineCompact.startsWith(key.compact)) {
      return Confidence.PREFIX
    }
    // Una letra mal leída: "HEPATOQRAMA"
    if (key.compact.length >= MIN_FUZZY_LENGTH && isNear(lineCompact, key.compact)) {
      return Confidence.FUZZY
    }
    return null
  }
  
  private fun isNear(lineCompact: String, keyCompact: String): Boolean =
    (keyCompact.length - 1..keyCompact.length + 1).any { size ->
      size <= lineCompact.length && levenshtein(lineCompact.take(size), keyCompact) <= 1
    }
  
  private companion object {
    const val MIN_COMPACT_PREFIX = 6
    const val MIN_FUZZY_LENGTH = 9
  }
}