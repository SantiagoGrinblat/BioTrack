package com.santidev.biotrack.domain.scanner

// Normaliza para comparar unidades: minúsculas, µ→u, ³→3, comillas→/, sin espacios ni punto final,
// y las confusiones típicas del OCR (i, 1 y | se leen como l).
private fun canon(raw: String): String =
  raw.lowercase()
    .replace('µ', 'u').replace('μ', 'u')
    .replace('³', '3')
    .replace('\'', '/').replace('’', '/').replace('`', '/').replace('\\', '/')
    .replace(Regex("""[*()\[\]{}:;,\s]"""), "")
    .trim('.')
    .replace('i', 'l').replace('1', 'l').replace('|', 'l')

// Las unidades que la app conoce. Cada una tiene el nombre con el que se guarda
// y las formas en que los laboratorios la escriben.

// Regla: las escrituras distintas de una MISMA notación se unifican (g/dL = gr/dL = g/dl).
// Las notaciones distintas quedan como unidades separadas, tal como las imprime cada
// laboratorio (x10³/µL no es lo mismo que mil/mm³, ni L/L lo mismo que %).
object UnitCatalog {
  
  private class Entry(val display: String, aliases: List<String>) {
    val keys: List<String> = (aliases + display).map { canon(it) }.distinct()
  }
  
  private val entries = listOf(
    Entry("g/dL", listOf("g/dl", "gdl", "gr/dl", "grdl")),
    Entry("g/L", listOf("g/l", "gr/l")),
    Entry("mg/dL", listOf("mg/dl", "mgdl")),
    Entry("mg/L", listOf("mg/l")),
    Entry("µg/dL", listOf("ug/dl", "mcg/dl", "mcgr/dl", "ugr/dl")),
    Entry("µg/L", listOf("ug/l", "mcg/l", "mcgr/l")),
    Entry("ng/dL", listOf("ng/dl")),
    Entry("ng/mL", listOf("ng/ml")),
    Entry("pg/mL", listOf("pg/ml")),
    Entry("%", listOf("%")),
    Entry("gr%", listOf("g%", "grs%", "gr96", "g96", "gr°/o", "g°/o")),
    Entry("mg%", listOf("mg96", "mg°/o")),
    Entry("L/L", listOf("l/l", "fraccion", "fracción")),
    Entry("/mm³", listOf("/mm3", "mm3", "mm", "/mm")),
    Entry("millones/mm³", listOf("millones/mm3", "mill/mm3", "millones/mm", "mill/mm", "10^6/mm3")),
    Entry("mil/mm³", listOf("mil/mm3", "mil/mm", "10^3/mm3")),
    Entry("/µL", listOf("/ul", "ul")),
    Entry("mill/µL", listOf("mill/ul", "millones/ul")),
    Entry("x10³/µL", listOf("x10^3/ul", "10^3/ul", "x103/ul", "10e3/ul", "x10e3/ul")),
    Entry("x10⁶/µL", listOf("x10^6/ul", "10^6/ul", "x106/ul", "10e6/ul", "x10e6/ul")),
    Entry("x10⁹/L", listOf("x10^9/l", "10^9/l", "x109/l", "10e9/l", "x10e9/l")),
    Entry("x10¹²/L", listOf("x10^12/l", "10^12/l", "x1012/l", "10e12/l", "x10e12/l")),
    Entry("/100 leucocitos", listOf("/100 leuc", "/100 wbc")),
    Entry("fL", listOf("fl", "femtolitros")),
    Entry("µm³", listOf("um3", "um", "u3", "mcm3")),
    Entry("pg", listOf("picogr", "picogramos", "pg/cel", "pg/celula", "pg/célula")),
    Entry("10(GSD)", listOf("10gsd", "gsd")),
    Entry("U/L", listOf("u/l")),
    Entry("UI/L", listOf("ui/l", "iu/l")),
    Entry("UI/mL", listOf("ui/ml", "iu/ml")),
    Entry("mUI/L", listOf("mui/l", "miu/l")),
    Entry("µUI/mL", listOf("uui/ml", "uiu/ml")),
    Entry("mmol/L", listOf("mmol/l")),
    Entry("mEq/L", listOf("meq/l")),
    Entry("mm/h", listOf("mm/hr", "mm/hora")),
    Entry("mL/min", listOf("ml/min")),
    Entry("seg", listOf("sec", "segs", "segundo", "segundos")),
    Entry("min", listOf("minuto", "minutos", "mins")),
  )
  
  // Para sugerir unidades en pantalla
  val all: List<String> get() = entries.map { it.display }
  
  // Busca la unidad al principio de un texto: prueba con 3, 2 y 1 palabras.
  // Devuelve null si no hay ninguna (no se inventa una unidad).
  fun find(text: String): String? {
    val tokens = text.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    for (n in minOf(3, tokens.size) downTo 1) {
      match(tokens.take(n).joinToString(""))?.let { return it }
    }
    return null
  }
  
  private fun match(raw: String): String? {
    val token = canon(raw)
    if (token.isEmpty()) return null
    
    entries.firstOrNull { token in it.keys }?.let { return it.display }
    
    // Parecido: una letra de diferencia (solo para unidades de 4+ caracteres)
    if (token.length >= 4) {
      val best = entries
        .flatMap { entry -> entry.keys.filter { it.length >= 4 }.map { it to entry.display } }
        .minByOrNull { (key, _) -> levenshtein(key, token) }
      if (best != null && levenshtein(best.first, token) <= 1) return best.second
    }
    return null
  }
}