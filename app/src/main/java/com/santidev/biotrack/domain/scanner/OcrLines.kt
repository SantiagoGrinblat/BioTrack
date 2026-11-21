package com.santidev.biotrack.domain.scanner

//que es cada linea que leyo el OCR = se limpia, se clasifica y se le da su posicion.
internal enum class Kind { NUMBER, UNIT, VALUE_UNIT, DESCRIPTION, NOTE, TEXT }

internal class Line(val source: OcrLine, val kind: Kind) {
  val text: String = cleanOcrText(source.text)
  val posX: Int get() = source.left
  val posY: Double get() = (source.top + source.bottom) / 2.0
}

//las lineas de la pagina, ya limpias y clasificadas, de arriba hacia abajo
internal fun classifyLines(page: OcrPage): List<Line> =
  page.lines
    .filter { cleanOcrText(it.text).isNotBlank() }
    .map { Line(it, kindOf(cleanOcrText(it.text))) }
    .sortedBy { it.posY }

private fun kindOf(text: String): Kind {
  if (numberRegex.matches(text)) return Kind.NUMBER
  //una unidad suelta, las palabras largas solo con letras (GLUCEMIA) no se toman como unidad.
  if (text.length <= MAX_UNIT_LENGTH && (text.any { !it.isLetter() } || text.length <= 4) &&
    UnitCatalog.find(text) != null
  ) return Kind.UNIT
  //(14,4 gr/dl) o (8680 mm) = valor y la unidad en la misma linea
  valueUnitRegex.matchEntire(text)?.let { match ->
    val rest = match.groupValues[2]
    if (rest.length <= MAX_UNIT_LENGTH && UnitCatalog.find(rest) != null) return Kind.VALUE_UNIT
  }
  if (compact(text).startsWith("metodo")) return Kind.DESCRIPTION
  if (':' in text || (text.first().isLowerCase() && text.length > 3)) return Kind.NOTE
  return Kind.TEXT
}

//esto saca del renglon, lo que el papel agrega y no es el dato =
//el valor de referencia (V.R.: hasta 34 U/L, tambien si viene despues de ":") y los ":" que quedan delante de un resultado (": 0.37 mg%")
private val referenceTail = Regex("""[\s:]*\bV\.?\s?R\.?\s*:.*$""", RegexOption.IGNORE_CASE)

private fun cleanOcrText(raw: String): String =
  raw.trim().replace(referenceTail, "").trimStart(':', ' ').trim()

private const val MAX_UNIT_LENGTH = 14
private val numberRegex = Regex("""^(?:[<>]=?\s*)?-?\d+(?:[.,]\d+)*$""")
internal val valueUnitRegex = Regex("""^((?:[<>]=?\s*)?-?\d+(?:[.,]\d+)*)\s*(\S.*)$""")