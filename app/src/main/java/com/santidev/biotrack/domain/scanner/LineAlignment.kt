package com.santidev.biotrack.domain.scanner

import kotlin.math.abs

//como se unen los nombres con los valores y como se agrupan las lineas entre las columnas medidas que comparten varios archivos
internal const val COLUMN_GAP = 0.06 //separacion maxima (del ancho) dentro de una columna
internal const val NAME_MARGIN = 0.2 //los nombres estan al menos asi de lejos a la izquierda
internal const val MAX_PAIR_DISTANCE = 250.0

private const val SKIP_UNKNOWN_NAME = 40.0
private const val SKIP_KNOWN_NAME = 400.0
private const val SKIP_VALUE = 300.0

private enum class Step { SKIP_NAME, SKIP_VALUE, MATCH }

//une nombres y valores en el mismo orden.
//saltar un nombre que no es de un resultado (un titulo) sale barato, re corrige rapido.
//saltar uno del catalogo sale caro y mas dificil de cambiar.
//saltar un valor, es el peor error.
internal fun align(
  names: List<Line>,
  isKnownName: List<Boolean>,
  values: List<Line>
): List<Pair<Int, Int>> {
  val nameCount = names.size
  val valueCount = values.size
  val cost = Array(nameCount + 1) { DoubleArray(valueCount + 1) { Double.MAX_VALUE } }
  val step = Array(nameCount + 1) { arrayOfNulls<Step>(valueCount + 1) }
  cost[0][0] = 0.0
  
  for (i in 0..nameCount) {
    for (j in 0..valueCount) {
      val current = cost[i][j]
      if (current == Double.MAX_VALUE) continue
      if (i < nameCount) {
        val next = current + if (isKnownName[i]) SKIP_KNOWN_NAME else SKIP_UNKNOWN_NAME
        if (next < cost[i + 1][j]) {
          cost[i + 1][j] = next; step[i + 1][j] = Step.SKIP_NAME
        }
      }
      if (j < valueCount) {
        val next = current + SKIP_VALUE
        if (next < cost[i][j + 1]) {
          cost[i][j + 1] = next; step[i][j + 1] = Step.SKIP_VALUE
        }
      }
      if (i < nameCount && j < valueCount) {
        val distance = abs(names[i].posY - values[j].posY)
        if (distance <= MAX_PAIR_DISTANCE && current + distance < cost[i + 1][j + 1]) {
          cost[i + 1][j + 1] = current + distance
          step[i + 1][j + 1] = Step.MATCH
        }
      }
    }
  }
  
  val pairs = mutableListOf<Pair<Int, Int>>()
  var nameIndex = nameCount
  var valueIndex = valueCount
  while (nameIndex > 0 || valueIndex > 0) {
    when (step[nameIndex][valueIndex]) {
      Step.MATCH -> {
        pairs += (nameIndex - 1) to (valueIndex - 1); nameIndex--; valueIndex--
      }
      
      Step.SKIP_NAME -> nameIndex--
      Step.SKIP_VALUE -> valueIndex--
      null -> return pairs.reversed()
    }
  }
  return pairs.reversed()
}

//nombres del catalogo y valores en el mismo orden. Si las cantidades no coinciden no se adivina nada.
internal fun alignByOrder(isKnownName: List<Boolean>, valueCount: Int): List<Pair<Int, Int>> {
  val knownIndexes = isKnownName.indices.filter { isKnownName[it] }
  if (knownIndexes.size != valueCount) return emptyList()
  return knownIndexes.mapIndexed { valueIndex, nameIndex -> nameIndex to valueIndex }
}

//cada elemento de (from) con el mas cercano en altura de (to), sin repetir ninguno
internal fun nearest(from: List<Line>, to: List<Line>, maxDistance: Double): Map<Line, Line> {
  val candidates = from.flatMap { source ->
    to.map { target -> Triple(abs(source.posY - target.posY), source, target) }
  }.sortedBy { it.first }
  val usedFrom = mutableSetOf<Line>()
  val usedTo = mutableSetOf<Line>()
  val result = mutableMapOf<Line, Line>()
  for ((distance, source, target) in candidates) {
    if (distance > maxDistance) break
    if (source in usedFrom || target in usedTo) continue
    usedFrom += source
    usedTo += target
    result[source] = target
  }
  return result
}

//grupos de lineas con casi la misma posicion horizontal
internal fun clusters(lines: List<Line>, gap: Double): List<List<Line>> {
  val result = mutableListOf<MutableList<Line>>()
  for (line in lines.sortedBy { it.posX }) {
    val last = result.lastOrNull()
    if (last != null && line.posX - last.last().posX <= gap) last += line else result += mutableListOf(line)
  }
  return result
}

internal fun median(values: List<Int>): Double {
  val sorted = values.sorted()
  val middle = sorted.size / 2
  return if (sorted.size % 2 == 1) sorted[middle].toDouble() else (sorted[middle - 1] + sorted[middle]) / 2.0
}