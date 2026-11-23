package com.santidev.biotrack.domain

import com.santidev.biotrack.model.Lab
import com.santidev.biotrack.model.LabResult
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

enum class Trend { UP, DOWN, SAME }

//resultado de comparar un estudio con el mismo estudio de un laboratorio anterior
data class Comparison(
  val previous: LabResult,
  val previousDate: Long,
  val trend: Trend,
  val difference: String //listo para mostrar, ej = 2,1%
)

//"7.629" = 7629 (punto de miles), "4,5" = 4,5 (coma decimal), "1.5" = 1,5
private val thousandsFormat = Regex("""-?[1-9]\d{0,2}(\.\d{3})+(,\d+)?""")
private val plainNumber = Regex("""-?\d+([.,]\d+)?""")

//devuelve null si el texto no es un numero puro ej = (Negativo O <0,5)
fun parseNumber(text: String): Double? {
  val impureNumber = text.trim()
  return when {
    thousandsFormat.matches(impureNumber) -> impureNumber.replace(".", "").replace(',', '.')
      .toDoubleOrNull()
    
    plainNumber.matches(impureNumber) -> impureNumber.replace(',', '.').toDoubleOrNull()
    else -> null
  }
}

private fun LabResult.isTime() = minutes != null || seconds != null

//numero para comparar los segundos totales en caso de que sea un tiempo, si no, el resultado
private fun LabResult.number(): Double? =
  if (isTime()) ((minutes ?: 0) * 60 + (seconds ?: 0)).toDouble() else parseNumber(value)

//es el mismo estudio, si coinciden nombre y unidad (sin importar mayusculas)
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

//compara con el laboratorio anterior mas cercano que tenga el mismo estudio.
//devuelve null si no hay anterior o si alguno de los dos no es un numero.
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

//un estudio del resumen = el ultimo valor obtenido y si hay, se compara con el valor anterior
data class SummaryItem(
  val result: LabResult,
  val date: Long,
  val comparison: Comparison?
)

//para cada estudio distinto (nombre + unidad), se toma el ultimo laboratorio donde aparece ese estudio
fun buildSummary(labs: List<Lab>): List<SummaryItem> {
  val seen = mutableSetOf<String>()
  val items = mutableListOf<SummaryItem>()
  labs.sortedByDescending { it.date }.forEach { lab ->
    lab.results.forEach { result ->
      val key =
        "${result.name.trim().lowercase()}|${result.unit.trim().lowercase()}|${result.isTime()}"
      if (seen.add(key)) items += SummaryItem(result, lab.date, compare(labs, lab, result))
    }
  }
  return items.sortedBy { it.result.name.trim().lowercase() }
}

//una carpeta del resumen = tiene su nombre y los estudios que tiene dentro de el
data class SummaryFolder(
  val name: String, // vacio = significa que no hay carpeta y queda "libre" en la lista
  val items: List<SummaryItem>
)

//"Hemograma", "hemograma" y "Hemogramá " son la misma carpeta
private fun folderKey(folder: String): String =
  Normalizer.normalize(folder.trim(), Normalizer.Form.NFD)
    .replace(Regex("""\p{M}+"""), "")
    .replace(Regex("""\s+"""), " ")
    .lowercase()

//agrupa el resumen de los estuidos por carpeta. Se muestra el nombre del primer estudio del grupo.
//las carpetas van por orden alfabetico y si son "sin carpeta", se pone al final de la lista.
fun groupByFolder(items: List<SummaryItem>): List<SummaryFolder> {
  val folders = items
    .groupBy { folderKey(it.result.folder) }
    .values
    .map { group -> SummaryFolder(group.first().result.folder.trim(), group) }
  return folders.sortedWith(compareBy<SummaryFolder>({ it.name.isEmpty() }, { folderKey(it.name) }))
}