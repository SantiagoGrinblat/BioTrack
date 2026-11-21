package com.santidev.biotrack.domain.scanner

import com.santidev.biotrack.domain.catalog.CatalogParameter
import kotlin.math.abs

// Qué parámetro del catálogo es el texto que leyó el OCR.
//   EXACT  = el texto es un nombre o sinónimo
//   PREFIX = coincide una parte del texto (sigla entre paréntesis, trozo entre guiones)
//            o el papel cortó el nombre
//   FUZZY  = hay una letra mal leída. Debe revisarse.
data class ParameterMatch(
  val parameter: CatalogParameter,
  val confidence: Confidence,
  val text: String   // el texto original, sin modificar
)

// Se crea con los parámetros de UN estudio (CatalogStudy.parameters).
// Es preferible no identificar un nombre antes que asociarlo mal.
class ParameterMatcher(parameters: List<CatalogParameter>) {
  
  private class Key(val parameter: CatalogParameter, val compact: String)
  
  private val keys: List<Key> = parameters
    .flatMap { p -> (p.aliases + p.name).distinct().map { Key(p, compact(it)) } }
    .filter { it.compact.isNotEmpty() }
  
  // Búsqueda exacta instantánea; si dos parámetros comparten un sinónimo, gana el primero
  private val exact: Map<String, CatalogParameter> = HashMap<String, CatalogParameter>().also { map ->
    keys.forEach { key -> if (key.compact !in map) map[key.compact] = key.parameter }
  }
  
  fun match(text: String): ParameterMatch? {
    val wholeForms = formsOf(text)
    
    // 1. El texto es un nombre o sinónimo: "HEMATOCRITO", "RECUENTO DE LEUCOCITOS"
    for (form in wholeForms) {
      exact[form]?.let { return ParameterMatch(it, Confidence.EXACT, text) }
    }
    
    // 2. Por partes: la sigla entre paréntesis, lo de afuera y los trozos entre guiones.
    //    "HEMOGLOB.CORPUSCULAR MEDIA(MCH)-HCM" -> MCH y HCM apuntan al mismo parámetro.
    //    Si las partes apuntan a parámetros distintos, es ambiguo y no se identifica.
    val found = partsOf(text)
      .mapNotNull { forms -> forms.firstNotNullOfOrNull { exact[it] } }
      .distinctBy { it.id }
    if (found.size == 1) return ParameterMatch(found.single(), Confidence.PREFIX, text)
    if (found.size > 1) return null
    
    // 3. El papel cortó el nombre: "CONCENTRACION HEMOGLOBINA CORP". Solo si es único.
    for (form in wholeForms) {
      if (form.length < MIN_CUT_LENGTH) continue
      val hits = keys.filter { it.compact.startsWith(form) }.map { it.parameter }.distinctBy { it.id }
      if (hits.size == 1) return ParameterMatch(hits.single(), Confidence.PREFIX, text)
    }
    
    // 4. Una letra mal leída
    return fuzzyMatch(text, wholeForms)
  }
  
  // Parecido por letras, con reglas estrictas para no confundir nombres distintos:
  // - la diferencia de largo es de una letra como máximo ("indirecta" no pasa por "directa")
  // - los números tienen que ser iguales (T3 no es T4)
  // - si dos parámetros empatan, no se elige ninguno
  private fun fuzzyMatch(text: String, forms: List<String>): ParameterMatch? {
    var best: CatalogParameter? = null
    var bestDistance = Int.MAX_VALUE
    var tie = false
    
    for (form in forms) {
      if (form.length < MIN_FUZZY_LENGTH) continue
      val maxDistance = if (form.length >= LONG_NAME) 2 else 1
      val formDigits = digitsOf(form)
      for (key in keys) {
        if (key.compact.length < MIN_FUZZY_LENGTH) continue
        if (abs(key.compact.length - form.length) > 1) continue
        if (digitsOf(key.compact) != formDigits) continue
        val distance = levenshtein(key.compact, form)
        if (distance > maxDistance) continue
        
        val current = best
        when {
          distance < bestDistance -> {
            best = key.parameter
            bestDistance = distance
            tie = false
          }
          distance == bestDistance && current != null && key.parameter.id != current.id -> tie = true
        }
      }
    }
    val result = best
    return if (result != null && !tie) ParameterMatch(result, Confidence.FUZZY, text) else null
  }
  
  // El texto sin espacios (el OCR parte palabras) y también sin el prefijo "recuento de"
  private fun formsOf(raw: String): List<String> {
    val normalized = normalize(raw)
    val withoutCount = normalized.removePrefix(COUNT_PREFIX)
    return listOf(normalized.replace(" ", ""), withoutCount.replace(" ", "")).distinct()
  }
  
  // Las partes de un nombre compuesto. Sin paréntesis ni guiones no hay partes.
  private fun partsOf(raw: String): List<List<String>> {
    if ('(' !in raw && '-' !in raw) return emptyList()
    val inside = parens.findAll(raw).map { it.groupValues[1] }.toList()
    val outside = raw.replace(parens, " ")
    val pieces = if ('-' in outside) outside.split('-') else emptyList()
    return (inside + outside + pieces).filter { it.isNotBlank() }.map { formsOf(it) }
  }
  
  private fun digitsOf(text: String) = text.filter { it.isDigit() }
  
  private companion object {
    const val COUNT_PREFIX = "recuento de "
    const val MIN_CUT_LENGTH = 15
    const val MIN_FUZZY_LENGTH = 5
    const val LONG_NAME = 12
    val parens = Regex("""\(([^)]*)\)""")
  }
}