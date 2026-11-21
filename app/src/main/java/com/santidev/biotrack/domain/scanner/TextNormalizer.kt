package com.santidev.biotrack.domain.scanner

import java.text.Normalizer

// Para comparar textos: minúsculas, sin tildes y cada signo pasa a espacio.
//   "HEPA TOGRAMA (AA)" -> "hepa tograma aa"
// Solo se usa para comparar: el texto leído por el OCR se conserva tal cual.
fun normalize(raw: String): String =
  Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("""\p{Mn}+"""), "")
    .replace(Regex("""[^a-z0-9]+"""), " ")
    .trim()

// Igual, pero sin espacios. Sirve cuando el OCR parte una palabra: "hepa tograma" -> "hepatograma"
fun compact(raw: String): String = normalize(raw).replace(" ", "")

// Cantidad de letras que hay que cambiar para pasar de un texto a otro (errores del OCR)
fun levenshtein(a: String, b: String): Int {
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