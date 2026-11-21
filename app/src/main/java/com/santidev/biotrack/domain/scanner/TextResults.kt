package com.santidev.biotrack.domain.scanner

//filas de resultados de texto y las lineas de valor que usaron (el flujo numerico no las vuelve a leer)
internal class TextRows(val placed: List<Placed>, val usedValues: Set<Line>)

//lee los resultados que son texto (AMARILLO, 1,010, ESCASOS (1-4/CPA)) = el valor es la linea entera de la columna de resultados.
//los encabezados (RESULTADO, VALOR DE REFERENCIA) marcan las columnas, lo que cae en la referencia se descarta.
internal class TextResultReader(private val matcher: ParameterMatcher) {
  
  private val tableHeaders =
    listOf("PRACTICAS SOLICITADAS", "RESULTADO", "UNIDAD", "VALOR DE REFERENCIA")
  
  private fun isTableHeader(line: Line): Boolean =
    tableHeaders.any { compact(it) == compact(line.text) }
  
  private fun headerX(lines: List<Line>, header: String): Int? =
    lines.firstOrNull { compact(it.text) == compact(header) }?.posX
  
  fun read(lines: List<Line>, inlineLines: Set<Line>, width: Double): TextRows {
    val textNames = lines
      .filter { it.kind == Kind.TEXT && it !in inlineLines }
      .mapNotNull { line ->
        matcher.match(line.text)?.takeIf { it.parameter.isText }?.let { line to it }
      }
    if (textNames.isEmpty()) return TextRows(emptyList(), emptySet())
    
    val resultHeaderX = headerX(lines, "RESULTADO")
    val referenceHeaderX = headerX(lines, "VALOR DE REFERENCIA")
    val columnGap = COLUMN_GAP * width
    
    //solo valores a la altura de los nombres de texto
    val firstNameY = textNames.minOf { it.first.posY } - MAX_PAIR_DISTANCE
    val lastNameY = textNames.maxOf { it.first.posY } + MAX_PAIR_DISTANCE
    
    val candidates = lines.filter {
      it.kind != Kind.UNIT && it.kind != Kind.DESCRIPTION &&
        it !in inlineLines && !isTableHeader(it) && it.posY in firstNameY..lastNameY
    }
    val unsortedValues = if (resultHeaderX != null) {
      candidates.filter {
        it.posX >= resultHeaderX - columnGap &&
          (referenceHeaderX == null || it.posX < referenceHeaderX - columnGap)
      }
    } else {
      //sin encabezado, la primera columna a la derecha de los nombres
      val nameX = median(textNames.map { it.first.posX })
      clusters(candidates.filter { it.posX > nameX + NAME_MARGIN * width }, columnGap)
        .firstOrNull().orEmpty()
    }
    val textValues = unsortedValues.sortedBy { it.posY }
    
    val pairs = align(textNames.map { it.first }, textNames.map { true }, textValues)
    val placed = pairs.map { (nameIndex, valueIndex) ->
      val (nameLine, match) = textNames[nameIndex]
      val row = ScannedRow(
        name = match.parameter.name,
        value = textValues[valueIndex].text,
        unit = "",
        description = "",
        needsReview = match.confidence == Confidence.FUZZY
      )
      Placed(nameLine.posY, listOf(row))
    }
    return TextRows(placed, pairs.map { textValues[it.second] }.toSet())
  }
}