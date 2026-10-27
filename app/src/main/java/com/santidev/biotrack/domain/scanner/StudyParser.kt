package com.santidev.biotrack.domain.scanner

import com.santidev.biotrack.domain.NUMBER_PATTERN
import com.santidev.biotrack.domain.parseNumber
import java.text.Normalizer

data class ParsedRow(
  val name: String,
  val valueText: String,
  val unit: String,
  val rangeMin: Double?,
  val rangeMax: Double?,
  val needsReview: Boolean
)

data class ParsedStudy(val titleSuggestion: String?, val rows: List<ParsedRow>)

// Parámetros frecuentes: nombre que se muestra → otras formas en que aparece (sin tildes, en minúscula)
private val NAME_CATALOG: Map<String, List<String>> = mapOf(
  "Leucocitos" to listOf("globulos blancos", "wbc", "GB"),
  "Eritrocitos" to listOf("globulos rojos", "hematies", "rbc"),
  "Hemoglobina" to listOf("hb", "hgb"),
  "Hematocrito" to listOf("hto", "hct"),
  "VCM" to listOf("mcv", "volumen corpuscular medio"),
  "HCM" to listOf("mch", "hemoglobina corpuscular media"),
  "CCMH" to listOf("chcm", "mchc"),
  "ADE" to listOf("rdw"),
  "Plaquetas" to listOf("plq", "plt"),
  "Neutrófilos" to listOf("neutrofilos totales"),
  "Linfocitos" to emptyList(),
  "Monocitos" to emptyList(),
  "Eosinófilos" to emptyList(),
  "Basófilos" to emptyList(),
  "Glucemia" to listOf("glucosa", "glicemia"),
  "Urea" to listOf("uremia"),
  "Creatinina" to listOf("creatininemia"),
  "Ácido úrico" to listOf("acido urico", "uricemia"),
  "Colesterol total" to listOf("colesterol"),
  "Colesterol HDL" to listOf("hdl", "hdl colesterol"),
  "Colesterol LDL" to listOf("ldl", "ldl colesterol"),
  "Triglicéridos" to listOf("trigliceridos"),
  "GOT" to listOf("ast", "tgo"),
  "GPT" to listOf("alt", "tgp"),
  "Fosfatasa alcalina" to listOf("fal"),
  "Bilirrubina total" to emptyList(),
  "Bilirrubina directa" to emptyList(),
  "Sodio" to listOf("natremia"),
  "Potasio" to listOf("potasemia", "kalemia"),
  "Cloro" to emptyList(),
  "Calcio" to listOf("calcemia"),
  "Magnesio" to emptyList(),
  "Hierro" to listOf("sideremia"),
  "Ferritina" to emptyList(),
  "TSH" to emptyList(),
  "T4 libre" to emptyList(),
  "Vitamina D" to listOf("25 oh vitamina d"),
  "Vitamina B12" to emptyList(),
  "Hemoglobina glicosilada" to listOf("hba1c", "hb a1c"),
  "Eritrosedimentación" to listOf("vsg", "velocidad de sedimentacion"),
  "Neutrófilos segmentados" to listOf("segmentados"),
  "Neutrófilos en cayado" to listOf("cayados", "baciliformes", "neutrofilos baciliformes"),
  "Metamielocitos" to emptyList(),
  "Mielocitos" to emptyList(),
  "Tiempo de coagulación" to emptyList(),
  "Tiempo de sangría" to emptyList(),
  "Tiempo de protrombina" to listOf("tp", "tiempo de quick"),
  "Tiempo de trombina" to listOf("tt"),
  "KPTT" to listOf("ttpa", "aptt", "tiempo de tromboplastina parcial activada"),
)

private val DIFFERENTIAL = setOf(
  "Neutrófilos", "Neutrófilos segmentados", "Neutrófilos en cayado",
  "Linfocitos", "Monocitos", "Eosinófilos", "Basófilos",
  "Metamielocitos", "Mielocitos"
)

private fun splitNameAndRest(cells: List<String>): Pair<String, String>? {
  val clean = cells.map { it.trim() }.filter { it.isNotEmpty() }
  return when {
    clean.size >= 2 -> clean.first() to clean.drop(1).joinToString(" ")
    clean.size == 1 -> singleCellRegex.matchEntire(clean[0])
      ?.let { it.groupValues[1] to it.groupValues[2] }
    else -> null
  }
}

// Fórmula leucocitaria: una fila trae el porcentaje y el valor absoluto
private fun parseDifferentialRow(cells: List<String>): List<ParsedRow>? {
  val (rawName, rest) = splitNameAndRest(cells) ?: return null
  // Solo contra el catálogo (lista vacía), para no confundirlo con parámetros ya guardados
  val (name, nameKnown) = resolveName(rawName, emptyList())
  if (!nameKnown || name !in DIFFERENTIAL) return null
  
  val matches = valueRegex.findAll(rest).toList()
  if (matches.size < 2) return null
  
  // "25  20 - 40" es un valor con su rango, no un porcentaje con su absoluto
  val afterFirst = rest.substring(matches[0].range.last + 1).trim()
  if (twoSidedRegex.find(afterFirst)?.range?.first == 0) return null
  
  val percent = matches[0].groupValues[2]
  val absolute = matches[1].groupValues[2]
  val percentNumber = parseNumber(percent) ?: return null
  if (percentNumber > 100) return null
  
  return listOf(
    ParsedRow("$name (%)", percent, "%", null, null, needsReview = false),
    ParsedRow("$name (absoluto)", absolute, "/mm³", null, null, needsReview = false)
  )
}

private fun normalizeName(raw: String): String =
  Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("""\p{Mn}+"""), "")
    .replace(Regex("""[^a-z0-9 ]"""), "")
    .replace(Regex("""\s+"""), " ")
    .trim()

private val catalogIndex: List<Pair<String, String>> = NAME_CATALOG.flatMap { (display, aliases) ->
  (aliases + display).map { normalizeName(it) to display }
}

private val singleCellRegex = Regex("""^(\D{2,}?)\s+(.*\d.*)$""")
private val dateRegex = Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""")
private val titleRegex = Regex("""^[\p{L} ]{4,40}$""")
private val valueRegex = Regex("""([<>]=?\s*)?($NUMBER_PATTERN)""")
private val twoSidedRegex =
  Regex("""($NUMBER_PATTERN)\s*(?:-|–|—|\ba\b|hasta)\s*($NUMBER_PATTERN)""")
private val oneSidedRegex = Regex("""([<>]=?)\s*($NUMBER_PATTERN)""")

private fun prettify(raw: String): String {
  val clean = raw.trim().trimEnd('.', ':', '-', '_', ' ').replace(Regex("""\s+"""), " ")
  // Siglas cortas (VCM, HCM) quedan en mayúscula; el resto, con la primera en mayúscula
  return if (clean.length <= 5 && !clean.contains(' ')) clean.uppercase()
  else clean.lowercase().replaceFirstChar { it.uppercase() }
}

private val compoundTimeRegex = Regex(
  """^\s*(\d+)\s*(?:minutos?|mins|min)\.?\s*(\d+)\s*(?:segundos?|segs|seg|sec)\.?""",
  RegexOption.IGNORE_CASE
)

// Devuelve el nombre a usar y si se reconoció
private fun resolveName(raw: String, knownNames: List<String>): Pair<String, Boolean> {
  val key = normalizeName(raw)
  val candidates = knownNames.map { normalizeName(it) to it } + catalogIndex
  
  candidates.firstOrNull { it.first == key }?.let { return it.second to true }
  
  if (key.length >= 5) {
    val maxDistance = if (key.length >= 9) 2 else 1
    val best = candidates
      .filter { it.first.length >= 5 }
      .minByOrNull { levenshtein(it.first, key) }
    if (best != null && levenshtein(best.first, key) <= maxDistance) return best.second to true
  }
  return prettify(raw) to false
}

fun parseStudy(rows: List<List<String>>, knownNames: List<String>): ParsedStudy {
  val parsedRows = rows.flatMap { parseRow(it, knownNames) }
  val title = rows.take(6)
    .firstOrNull { it.size == 1 && titleRegex.matches(it[0].trim()) }
    ?.get(0)?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() }
  return ParsedStudy(title, parsedRows)
}

private fun parseSingleRow(cells: List<String>, knownNames: List<String>): ParsedRow? {
  val (rawName, rest) = splitNameAndRest(cells) ?: return null
  if (rawName.count { it.isLetter() } < 2) return null
  if (dateRegex.containsMatchIn(rest)) return null
  
  val (name, nameKnown) = resolveName(rawName, knownNames)
  
  compoundTimeRegex.find(rest)?.let { m ->
    return ParsedRow(
      name = name,
      valueText = "${m.groupValues[1]} min ${m.groupValues[2]} seg",
      unit = "",
      rangeMin = null,
      rangeMax = null,
      needsReview = !nameKnown
    )
  }
  
  val valueMatch = valueRegex.find(rest)
  if (valueMatch == null) {
    // Valor de texto (ej. "Negativo"): solo se acepta si el nombre es reconocido
    if (!nameKnown || rest.length > 40) return null
    return ParsedRow(name, rest, "", null, null, needsReview = true)
  }
  
  val valueText = valueMatch.groupValues[1].replace(" ", "") + valueMatch.groupValues[2]
  val after = rest.substring(valueMatch.range.last + 1).trim()
  
  // Rango de referencia: "4.500 - 11.000", "< 200" o "> 40"
  var rangeMin: Double? = null
  var rangeMax: Double? = null
  var unitSource = after
  val two = twoSidedRegex.find(after)
  val one = if (two == null) oneSidedRegex.find(after) else null
  if (two != null) {
    rangeMin = parseNumber(two.groupValues[1])
    rangeMax = parseNumber(two.groupValues[2])
    unitSource = after.replace(two.value, " ")
  } else if (one != null) {
    val limit = parseNumber(one.groupValues[2])
    if (one.groupValues[1].startsWith("<")) rangeMax = limit else rangeMin = limit
    unitSource = after.replace(one.value, " ")
  }
  
  val unit = UnitCatalog.find(unitSource)
  
  // Si no hay ninguna señal (nombre, unidad o rango), es ruido del papel
  if (!nameKnown && unit == null && rangeMin == null && rangeMax == null) return null
  
  return ParsedRow(
    name = name,
    valueText = valueText,
    unit = unit.orEmpty(),
    rangeMin = rangeMin,
    rangeMax = rangeMax,
    needsReview = !nameKnown || unit == null
  )
}

private fun parseRow(cells: List<String>, knownNames: List<String>): List<ParsedRow> {
  parseDifferentialRow(cells)?.let { return it }
  return listOfNotNull(parseSingleRow(cells, knownNames))
}