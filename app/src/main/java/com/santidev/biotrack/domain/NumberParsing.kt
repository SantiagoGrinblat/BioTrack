package com.santidev.biotrack.domain

// "6.500" = 6500 (punto de miles) · "4,5" = 4,5 (coma decimal) · "1.5" = 1,5
const val NUMBER_PATTERN = """-?[1-9]\d{0,2}(?:\.\d{3})+(?:,\d+)?|-?\d+(?:[.,]\d+)?"""

private val fullNumber = Regex(NUMBER_PATTERN)
private val thousandsFormat = Regex("""-?[1-9]\d{0,2}(?:\.\d{3})+(?:,\d+)?""")

fun parseNumber(text: String): Double? {
  val t = text.trim()
  if (!fullNumber.matches(t)) return null
  val normalized = if (thousandsFormat.matches(t)) {
    t.replace(".", "").replace(',', '.')
  } else {
    t.replace(',', '.')
  }
  return normalized.toDoubleOrNull()
}