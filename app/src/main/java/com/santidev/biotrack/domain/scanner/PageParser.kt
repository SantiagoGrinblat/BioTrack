package com.santidev.biotrack.domain.scanner

import com.santidev.biotrack.domain.catalog.CatalogParameter
import kotlin.math.abs

//una fila hace la lista para pasar al formulario. nada se guarda directo. (el usuario lo tiene que revisar).
data class ScannedRow(
  val name: String, //el nombre del catalogo si se reconocio, si no, el texto leido
  val value: String, //tal cual como esta en el papel
  val unit: String,
  val description: String, //por ejemplo el metodo con que se midio el estudio
  val needsReview: Boolean //en caso de dudosa lectura = se pinta de color rojo en el formulario
)

//una o mas filas con la altura de su nombre, para ordenarlas al final de arriba hacia abajo
internal class Placed(val posY: Double, val rows: List<ScannedRow>)

//!convierte las lineas de una pagina (texto + posicion) en filas = nombre, valor y unidad.
//los informes en tabla pueden salir inclinados, asi que NO se empareja por "el mas cercano en altura" (proque eso falla).
//se separan las lineas por lo que son (nombre, numero, unidad, metodo).
//y se emparejan en el mismo orden de arriba hacia abajo con un alineamiento, que tolera lineas de mas O de menos.
//!lo que no cierra se marca para revisar y nunca se inventa un texto O numero.
class PageParser(private val matcher: ParameterMatcher) {
  
  private val textReader = TextResultReader(matcher)
  
  //un numero con su unidad debajo, en la columna de unidades =
  //el valor absoluto de la formula leucocitaria (el porcentaje va en la columna de resultados).
  private class Stacked(val number: Line, val unit: Line)
  
  fun parse(page: OcrPage): List<ScannedRow> {
    val lines = classifyLines(page)
    val width = page.width.toDouble()
    val placed = mutableListOf<Placed>()
    
    //filas que vienen enteras en una sola linea = Hemoglobina 14,2 g/dL
    val inlineLines = mutableSetOf<Line>()
    for (line in lines.filter { it.kind == Kind.TEXT }) {
      val row = inlineRow(line.text) ?: continue
      inlineLines += line
      placed += Placed(line.posY, listOf(row))
    }
    
    //resultados de texto (orina) = usan su propia lectura y sus valores no vuelven al flujo numerico
    val textResult = textReader.read(lines, inlineLines, width)
    placed += textResult.placed
    
    //columna de resultados = el grupo de numeros mas numeroso.
    val numericLines = lines.filter {
      (it.kind == Kind.NUMBER || it.kind == Kind.VALUE_UNIT) && it !in textResult.usedValues
    }
    val resultColumn = clusters(numericLines, COLUMN_GAP * width).maxByOrNull { it.size }
    if (resultColumn == null) return placed.sortedBy { it.posY }.flatMap { it.rows }
    
    val resultColumnSet = resultColumn.toSet()
    val valueX = median(resultColumn.map { it.posX })
    val values = resultColumn.sortedBy { it.posY }
    
    //columna de las unidades
    val units = lines.filter { it.kind == Kind.UNIT }
    val unitX = if (units.isEmpty()) null else median(units.map { it.posX })
    val numbersInUnitColumn = numericLines.filter {
      it !in resultColumnSet && it.kind == Kind.NUMBER && unitX != null && abs(it.posX - unitX) <= UNIT_TOLERANCE * width
    }.toSet()
    
    //numero + unidad apilados en la columna de unidades
    val unitColumn = (units + numbersInUnitColumn).sortedBy { it.posY }
    val stacked = mutableListOf<Stacked>()
    val consumed = mutableSetOf<Line>()
    unitColumn.forEachIndexed { index, item ->
      val next = unitColumn.getOrNull(index + 1)
      if (item in numbersInUnitColumn && next != null && next.kind == Kind.UNIT && next.posY - item.posY <= STACK_GAP) {
        stacked += Stacked(item, next)
        consumed += next
      }
    }
    
    //cada resultado con su unidad (la mas cercana en altura, sin repetirse)
    val needUnit = values.filter { it.kind == Kind.NUMBER }
    val unitOf = nearest(needUnit, units.filter { it !in consumed }, PAIR_DISTANCE)
    val stackedOf: Map<Line, Stacked> = nearest(values, stacked.map { it.number }, PAIR_DISTANCE)
      .mapNotNull { (value, number) ->
        stacked.firstOrNull { it.number === number }?.let { value to it }
      }
      .toMap()
    
    //nombres = texto a la izquierda de la columna de resultados
    val names = lines.filter {
      it.kind == Kind.TEXT && it !in inlineLines && it.posX < valueX - NAME_MARGIN * width &&
        matcher.match(it.text)?.parameter?.isText != true
    }
    val matches = names.map { matcher.match(it.text) }
    val isKnownName = matches.map { it != null }
    val heightPairs = align(names, isKnownName, values)
    //si los resultados vienen en otro bloque no hay emparejamiento por altura.
    //se emparejan por orden, solo si hay tantos nombres del catalogo como valores
    val pairs = if (heightPairs.size < values.size) {
      alignByOrder(isKnownName, values.size).ifEmpty { heightPairs }
    } else {
      heightPairs
    }
    val pairedNames = pairs.map { names[it.first] }.toSet()
    
    //lineas de metodo (descripcion) para asociarlas con el nombre de arriba
    val leftColumn = lines.filter {
      (it.kind == Kind.TEXT || it.kind == Kind.DESCRIPTION || it.kind == Kind.NOTE) &&
        it !in inlineLines && it.posX < valueX - NAME_MARGIN * width
    }
    val descriptionOf = mutableMapOf<Line, String>()
    leftColumn.forEachIndexed { index, line ->
      if (line.kind != Kind.DESCRIPTION || index == 0) return@forEachIndexed
      val previous = leftColumn[index - 1]
      if (previous !in pairedNames) return@forEachIndexed
      //si el metodo sigue a dos nombres seguidos, entonces no se sabe de cual es = no se asocia a ninguno de los 2
      val before = leftColumn.getOrNull(index - 2)
      if (before != null && before in pairedNames && before !in descriptionOf) return@forEachIndexed
      descriptionOf[previous] = cleanDescription(line.text)
    }
    
    for ((nameIndex, valueIndex) in pairs) {
      val nameLine = names[nameIndex]
      val valueLine = values[valueIndex]
      val match = matches[nameIndex]
      
      var review = match == null || match.confidence == Confidence.FUZZY
      val name = match?.parameter?.name ?: prettify(nameLine.text)
      val expected = match?.parameter?.let { expectedUnits(it) } ?: emptySet()
      
      val valueText: String
      var unit: String?
      if (valueLine.kind == Kind.VALUE_UNIT) {
        val valueUnitParts = valueUnitRegex.matchEntire(valueLine.text)
        valueText = valueUnitParts?.groupValues?.get(1)?.trim() ?: valueLine.text
        unit = valueUnitParts?.groupValues?.get(2)?.let { UnitCatalog.find(it) }
      } else {
        valueText = valueLine.text
        unit = unitOf[valueLine]?.let { UnitCatalog.find(it.text) }
      }
      val absolute = stackedOf[valueLine]
      
      when {
        //la unidad se leyo del papel, si no es la esperada, la revisa el usuario
        unit != null -> if (expected.isNotEmpty() && unit !in expected) review = true
        //hay un valor absoluto en la misma fila = el resultado es un porcentaje (siempre)
        absolute != null -> {
          unit = "%"
          if (expected.isNotEmpty() && "%" !in expected) review = true
        }
        //no se leyo = se sugiere la del catalogo si hay una sola, etnonces se revisa
        expected.size == 1 -> {
          unit = expected.single()
          review = true
        }
        
        else -> {
          unit = ""
          if (expected.isNotEmpty()) review = true
        }
      }
      
      val rows = mutableListOf(
        ScannedRow(
          name,
          valueText,
          unit.orEmpty(),
          descriptionOf[nameLine].orEmpty(),
          review
        )
      )
      if (absolute != null) {
        val absoluteUnit = UnitCatalog.find(absolute.unit.text)
        rows += ScannedRow(
          "$name (absoluto)",
          absolute.number.text,
          absoluteUnit.orEmpty(),
          "",
          absoluteUnit == null
        )
      }
      placed += Placed(nameLine.posY, rows)
    }
    
    return placed.sortedBy { it.posY }.flatMap { it.rows }
  }
  
  //(Hemoglobina 14,2 gr/dL) = solo si el nombre es parte de los sinonimos del catalogo
  private fun inlineRow(text: String): ScannedRow? {
    val inlineParts = inlineRegex.matchEntire(text) ?: return null
    val match = matcher.match(inlineParts.groupValues[1]) ?: return null
    val expected = expectedUnits(match.parameter)
    val readUnit = UnitCatalog.find(inlineParts.groupValues[3])
    
    var review = match.confidence == Confidence.FUZZY
    val unit: String = when {
      readUnit != null -> {
        if (expected.isNotEmpty() && readUnit !in expected) review = true
        readUnit
      }
      
      expected.size == 1 -> {
        review = true
        expected.single()
      }
      
      else -> {
        if (expected.isNotEmpty()) review = true
        ""
      }
    }
    return ScannedRow(match.parameter.name, inlineParts.groupValues[2], unit, "", review)
  }
  
  //!textos
  
  private fun expectedUnits(parameter: CatalogParameter): Set<String> =
    parameter.units.mapNotNull { UnitCatalog.find(it) }.toSet()
  
  //"METODO:1ON DIAZONIUM - ARCHITECT C8000/4000- TRANSFERENCIA AUTOMATICA" -> "Metodo: 1ON DIAZONIUM - ARCHITECT C8000/4000"
  private fun cleanDescription(text: String): String =
    "Método: " + text.replace(methodPrefix, "").replace(transferSuffix, "").trim(' ', '-')
  
  private fun prettify(raw: String): String {
    val clean = raw.trim().trimEnd('.', ':', '-', '_', ' ').replace(Regex("""\s+"""), " ")
    return if (clean.length <= 5 && ' ' !in clean) clean.uppercase()
    else clean.lowercase().replaceFirstChar { it.uppercase() }
  }
  
  //no se bien como funcionan estos 3 me lo sugerio cloud, pero hace que funcione y not tegna los erorres de antes.
  //todo = INVESTIGAR EL FUNCIONAMIENTO LOGICO DE ESTO
  private companion object {
    const val UNIT_TOLERANCE = 0.08
    const val STACK_GAP = 140.0 //alto maximo entre un numero y su unidad apilada
    const val PAIR_DISTANCE = 60.0
    
    val inlineRegex = Regex("""^(\D{3,}?)\s+((?:[<>]=?\s*)?-?\d+(?:[.,]\d+)*)\s*(.*)$""")
    val methodPrefix = Regex("""^\s*meto\s*do\s*[:0]*\s*""", RegexOption.IGNORE_CASE)
    val transferSuffix = Regex("""\s*-?\s*TRANSF\w*.*$""", RegexOption.IGNORE_CASE)
  }
}