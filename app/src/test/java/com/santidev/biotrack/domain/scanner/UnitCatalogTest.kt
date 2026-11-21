package com.santidev.biotrack.domain.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitCatalogTest {
  
  private fun assertUnit(expected: String, text: String) =
    assertEquals("Texto leído: $text", expected, UnitCatalog.find(text))
  
  @Test
  fun `las escrituras de una misma notacion dan la misma unidad`() {
    listOf("g/dL", "g/dl", "G/DL", "gr/dL", "gr/dL.").forEach { assertUnit("g/dL", it) }
    listOf("mg/dL", "mg/dl", "mg/dL.").forEach { assertUnit("mg/dL", it) }
  }
  
  @Test
  fun `las unidades de los informes reales se reconocen`() {
    assertUnit("/mm³", "/mm3.")
    assertUnit("fL", "fL.")
    assertUnit("pg", "picogr.")
    assertUnit("10(GSD)", "10(GSD)")
    assertUnit("µg/dL", "mcgr/dL")
    assertUnit("U/L", "U/L")
    assertUnit("%", "%")
    assertUnit("mm/h", "mm/h")
  }
  
  @Test
  fun `las notaciones distintas quedan como unidades separadas`() {
    assertUnit("mil/mm³", "mil/mm3")
    assertUnit("x10³/µL", "x10^3/µL")
    assertUnit("x10⁹/L", "x10^9/L")
    assertNotEquals(UnitCatalog.find("mil/mm3"), UnitCatalog.find("x10^9/L"))
    assertNotEquals(UnitCatalog.find("%"), UnitCatalog.find("L/L"))
    assertUnit("millones/mm³", "mill/mm3")
    assertUnit("mill/µL", "mill/ul")
  }
  
  @Test
  fun `las confusiones del OCR se corrigen`() {
    assertUnit("mg/dL", "mg/di")       // la l leída como i
    assertUnit("g/dL", "g'dlI")        // comilla y mayúscula
  }
  
  @Test
  fun `la unidad se busca al principio de un texto con otras cosas`() {
    assertUnit("/mm³", "mm3 4.00 5.20")
    assertUnit("g/dL", "g/dl 11,8-14,8")
  }
  
  @Test
  fun `los tiempos se reconocen`() {
    assertUnit("seg", "seg")
    assertUnit("min", "minutos")
  }
  
  @Test
  fun `si no hay unidad no se inventa una`() {
    assertNull(UnitCatalog.find("8680"))
    assertNull(UnitCatalog.find("Negativo"))
    assertNull(UnitCatalog.find(""))
  }
}