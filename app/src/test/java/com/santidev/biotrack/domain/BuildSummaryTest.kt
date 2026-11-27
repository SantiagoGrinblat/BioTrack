package com.santidev.biotrack.domain

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class BuildSummaryTest {
  
  @Test
  fun `un estudio repetido aparece una sola vez con el ultimo valor y su comparacion`() {
    val summary = buildSummary(
      listOf(lab(1, labResult(value = "92")), lab(2, labResult(value = "105")))
    )
    val item = summary.single()
    assertEquals("105", item.result.value)
    assertEquals(2L, item.date)
    assertEquals(Trend.UP, item.comparison?.trend)
  }
  
  @Test
  fun `el orden en que llegan los laboratorios no importa`() {
    val a = lab(1, labResult(value = "92"))
    val b = lab(2, labResult(value = "105"))
    assertEquals(buildSummary(listOf(a, b)), buildSummary(listOf(b, a)))
  }
  
  @Test
  fun `un estudio que solo esta en un laboratorio viejo igual aparece, con su fecha y sin comparacion`() {
    val summary = buildSummary(
      listOf(
        lab(1, labResult(name = "Urea", value = "30"), labResult(value = "92")),
        lab(2, labResult(value = "105"))
      )
    )
    val urea = summary.first { it.result.name == "Urea" }
    assertEquals(1L, urea.date)
    assertNull(urea.comparison)
  }
  
  @Test
  fun `el mismo nombre con otra unidad es otro estudio`() {
    val summary = buildSummary(
      listOf(
        lab(
          1,
          labResult(name = "Hemoglobina", value = "14", unit = "g/dL"),
          labResult(name = "Hemoglobina", value = "42", unit = "%")
        )
      )
    )
    assertEquals(2, summary.size)
  }
  
  @Test
  fun `si el estudio esta dos veces en el mismo laboratorio queda el primero`() {
    val summary = buildSummary(listOf(lab(1, labResult(value = "92"), labResult(value = "95"))))
    assertEquals(listOf("92"), summary.map { it.result.value })
  }
  
  @Test
  fun `sin laboratorios el resumen esta vacio`() {
    assertTrue(buildSummary(emptyList()).isEmpty())
  }
  
  @Test
  fun `se ordena alfabeticamente sin importar mayusculas`() {
    val summary = buildSummary(
      listOf(lab(1, labResult(name = "urea", value = "30"), labResult(name = "Ferritina", value = "80", unit = "ng/mL")))
    )
    assertEquals(listOf("Ferritina", "urea"), summary.map { it.result.name })
  }
  
  // HOY FALLA: "células" queda despues de "cristales" porque la é pesa mas que la z
  @Test
  fun `el orden alfabetico ignora las tildes`() {
    val summary = buildSummary(
      listOf(
        lab(
          1,
          labResult(name = "Cristales", value = "x", unit = ""),
          labResult(name = "Células epiteliales", value = "x", unit = ""),
          labResult(name = "Cilindros", value = "x", unit = "")
        )
      )
    )
    assertEquals(
      listOf("Células epiteliales", "Cilindros", "Cristales"),
      summary.map { it.result.name }
    )
  }
}