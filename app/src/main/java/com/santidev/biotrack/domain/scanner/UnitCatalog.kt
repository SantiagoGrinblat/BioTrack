package com.santidev.biotrack.domain.scanner

internal fun levenshtein(a: String, b: String): Int {
  val dp = IntArray(b.length + 1) { it }
  for (i in 1..a.length) {
    var prev = dp[0]
    dp[0] = i
    for (j in 1..b.length) {
      val temp = dp[j]
      dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
      prev = temp
    }
  }
  return dp[b.length]
}

// Normaliza para comparar: minúsculas, µ→u, ³→3, comillas→/, y las confusiones
// típicas del OCR (i, 1 y | se leen como l)
private fun canon(raw: String): String =
  raw.lowercase()
    .replace('µ', 'u').replace('μ', 'u')
    .replace('³', '3')
    .replace('\'', '/').replace('’', '/').replace('`', '/').replace('\\', '/')
    .replace(Regex("""[*()\[\]{}:;,\s]"""), "")
    .trim('.')
    .replace('i', 'l').replace('1', 'l').replace('|', 'l')

object UnitCatalog {
  
  private class Entry(val display: String, aliases: List<String>) {
    val keys: List<String> = (aliases + display).map { canon(it) }.distinct()
  }
  
  private val entries = listOf(
    Entry("mg/dL", listOf("mg/dl", "mgdl")),
    Entry("g/dL", listOf("g/dl", "gdl")),
    Entry("g/L", listOf("g/l")),
    Entry("mg/L", listOf("mg/l")),
    Entry("%", listOf("%")),
    Entry("/mm³", listOf("/mm3", "mm3", "mm", "/mm")),
    Entry("millones/mm³", listOf("millones/mm3", "mill/mm3", "millones/mm", "mill/mm")),
    Entry("mil/mm³", listOf("mil/mm3", "mil/mm")),
    Entry("/µL", listOf("/ul", "ul")),
    Entry("x10³/µL", listOf("x10^3/ul", "10^3/ul", "x103/ul", "10e3/ul", "x10e3/ul")),
    Entry("x10⁶/µL", listOf("x10^6/ul", "10^6/ul", "x106/ul", "10e6/ul", "x10e6/ul")),
    Entry("fL", listOf("fl")),
    Entry("µm³", listOf("um3", "um", "u3", "mcm3")),
    Entry("pg", listOf("pg")),
    Entry("U/L", listOf("u/l")),
    Entry("UI/L", listOf("ui/l", "iu/l")),
    Entry("UI/mL", listOf("ui/ml", "iu/ml")),
    Entry("mUI/L", listOf("mui/l", "miu/l")),
    Entry("µUI/mL", listOf("uui/ml", "uiu/ml")),
    Entry("mmol/L", listOf("mmol/l")),
    Entry("mEq/L", listOf("meq/l")),
    Entry("ng/mL", listOf("ng/ml")),
    Entry("ng/dL", listOf("ng/dl")),
    Entry("pg/mL", listOf("pg/ml")),
    Entry("µg/dL", listOf("ug/dl", "mcg/dl")),
    Entry("µg/L", listOf("ug/l", "mcg/l")),
    Entry("seg", listOf("seg", "sec")),
    Entry("mm/h", listOf("mm/h", "mm/hr", "mm/hora")),
    Entry("mL/min", listOf("ml/min")),
    Entry("gr%", listOf("g%", "grs%", "gr96", "g96", "gr°/o", "g°/o")),
    Entry("mg%", listOf("mg96", "mg°/o")),
    Entry("min", listOf("minuto", "minutos", "mins")),
    Entry("seg", listOf("sec", "segs", "segundo", "segundos")),
  )
  
  // Para sugerir unidades en pantalla más adelante
  val all: List<String> get() = entries.map { it.display }
  
  // Busca la unidad en un texto: prueba con 3, 2 y 1 palabras
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