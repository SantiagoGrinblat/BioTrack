package com.santidev.biotrack.domain

import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.model.LabResult
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class Trend { UP, DOWN, SAME }

// Resultado de comparar un estudio con el mismo estudio de un laboratorio anterior
data class Comparison(
  val previous: LabResult,
  val previousDate: Long,
  val trend: Trend,
  val difference: String   // ya listo para mostrar, ej. "2,1 %"
)

// "7.629" = 7629 (punto de miles), "4,5" = 4,5 (coma decimal), "1.5" = 1,5
private val thousandsFormat = Regex("""-?[1-9]\d{0,2}(\.\d{3})+(,\d+)?""")
private val plainNumber = Regex("""-?\d+([.,]\d+)?""")

// Devuelve null si el texto no es un número puro (ej. "Negativo" o "<0,5")
fun parseNumber(text: String): Double? {
  val t = text.trim()
  return when {
    thousandsFormat.matches(t) -> t.replace(".", "").replace(',', '.').toDoubleOrNull()
    plainNumber.matches(t) -> t.replace(',', '.').toDoubleOrNull()
    else -> null
  }
}

private fun LabResult.isTime() = minutes != null || seconds != null

// Número para comparar: los segundos totales si es un tiempo; si no, el resultado
private fun LabResult.number(): Double? =
  if (isTime()) ((minutes ?: 0) * 60 + (seconds ?: 0)).toDouble() else parseNumber(value)

// Es el mismo estudio si coinciden nombre y unidad (sin importar mayúsculas)
private fun LabResult.sameStudy(other: LabResult) =
  name.trim().equals(other.name.trim(), ignoreCase = true) &&
    unit.trim().equals(other.unit.trim(), ignoreCase = true) &&
    isTime() == other.isTime()

private val numberFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale("es", "AR")))

private fun formatDifference(amount: Double, result: LabResult): String =
  if (result.isTime()) {
    val total = amount.toInt()
    if (total >= 60) "${total / 60} min ${total % 60} seg" else "$total seg"
  } else {
    "${numberFormat.format(amount)} ${result.unit}".trim()
  }

// Compara con el laboratorio anterior más cercano que tenga el mismo estudio.
// Devuelve null si no hay anterior o si alguno de los dos no es un número.
fun compare(labs: List<Lab>, lab: Lab, result: LabResult): Comparison? {
  val current = result.number() ?: return null
  
  val found = labs
    .filter { it.date < lab.date }
    .sortedByDescending { it.date }
    .firstNotNullOfOrNull { older ->
      older.results.firstNotNullOfOrNull { candidate ->
        val number = candidate.number()
        if (number != null && candidate.sameStudy(result)) Triple(candidate, older.date, number)
        else null
      }
    } ?: return null
  
  val (previous, previousDate, before) = found
  val difference = current - before
  val trend = when {
    abs(difference) < 1e-9 -> Trend.SAME
    difference > 0 -> Trend.UP
    else -> Trend.DOWN
  }
  return Comparison(previous, previousDate, trend, formatDifference(abs(difference), result))
}

// Un estudio del resumen: su último valor y, si hay, la comparación con el anterior
data class SummaryItem(
  val result: LabResult,
  val date: Long,
  val comparison: Comparison?
)

// Para cada estudio distinto (nombre + unidad), toma el último laboratorio donde aparece
fun buildSummary(labs: List<Lab>): List<SummaryItem> {
  val seen = mutableSetOf<String>()
  val items = mutableListOf<SummaryItem>()
  labs.sortedByDescending { it.date }.forEach { lab ->
    lab.results.forEach { result ->
      val key = "${result.name.trim().lowercase()}|${result.unit.trim().lowercase()}|${result.isTime()}"
      if (seen.add(key)) items += SummaryItem(result, lab.date, compare(labs, lab, result))
    }
  }
  return items.sortedBy { it.result.name.trim().lowercase() }
}