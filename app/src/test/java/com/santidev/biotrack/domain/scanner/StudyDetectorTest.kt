package com.santidev.biotrack.domain.scanner

import com.santidev.biotrack.domain.catalog.StudyEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyDetectorTest {
  
  private fun entry(id: String, name: String, vararg aliases: String) =
    StudyEntry(id, name, aliases.toList(), "estudios/$id.json")
  
  private val detector = StudyDetector(
    listOf(
      entry("hemograma", "Hemograma", "hemograma completo", "recuento sanguineo"),
      entry("hepatograma", "Hepatograma", "perfil hepatico"),
      entry("ferremia", "Ferremia", "hierro serico", "sideremia"),
      entry("glucemia", "Glucemia", "glucosa en sangre"),
      entry("glucemia_posprandial", "Glucemia posprandial", "glucemia post prandial"),
      entry("uremia", "Uremia", "urea"),
      entry("hemoglobina_glicosilada", "Hemoglobina glicosilada", "hba1c")
    )
  )
  
  private fun ids(lines: List<String>) = detector.detect(lines).map { it.study.id }
  
  // Líneas de un informe real (sin datos personales)
  private val informe = listOf(
    "PRACTICAS SU",
    "HEMOGRAMA (AA)",
    "SERTE ROJA: Recuento e indices hematimetricos",
    "RECUENTO DE GLOBULOS ROJOS",
    "HEMATOCRITO",
    "HEMOGLOBINA",
    "VOLUMEN CORPUS CULAR MEDIO (VCM)",
    "HEMOGLOB.CORPUSCULAR MEDIA(MCH)-HCM",
    "INDICE DISTRIB. ERITROC.(RDW)",
    "SERIE BLANCA: Recuento y formula leucocitaria",
    "RECUENTO DE LEUCOCITOS",
    "NEUTROFILOS SEGMENTADOS",
    "OBSERVA CIONES",
    "FERREMIA",
    "METODO : FERENE - ARCHITECT C8000/4000- TRANSFERENCIA AUTOMATICA DE RESULTA DOS POR INTERFAZ (AA)",
    "GLUCEMIA",
    "METODO0:TRINDER - ARCHITECT C8000/4000- TRANSFERENCIA AUTOMATICA DE RESULTADOS POR INTERFAZ (AA)",
    "UREMIA",
    "METODO:UREASA, UV - ARCHITECT C8000/4000- TRANSFERENCIA AUTOMATICA DE RESULTADOS POR INTERFAZ (AA)"
  )
  
  @Test
  fun `un informe con varios estudios los devuelve todos en orden`() {
    assertEquals(listOf("hemograma", "ferremia", "glucemia", "uremia"), ids(informe))
  }
  
  @Test
  fun `la linea de un parametro no se toma como estudio`() {
    assertEquals(emptyList<String>(), ids(listOf("RECUENTO DE LEUCOCITOS", "HEMOGLOBINA", "HEMATOCRITO")))
  }
  
  @Test
  fun `un titulo con sigla al final se reconoce`() {
    val match = detector.detect(listOf("HEMOGRAMA (AA)")).single()
    assertEquals("hemograma", match.study.id)
    assertEquals(Confidence.PREFIX, match.confidence)
  }
  
  @Test
  fun `una palabra partida por el OCR se reconoce`() {
    val match = detector.detect(listOf("HEPA TOGRAMA (AA)")).single()
    assertEquals("hepatograma", match.study.id)
  }
  
  @Test
  fun `una letra mal leida se reconoce pero queda como dudosa`() {
    val match = detector.detect(listOf("HEPATOQRAMA")).single()
    assertEquals("hepatograma", match.study.id)
    assertEquals(Confidence.FUZZY, match.confidence)
  }
  
  @Test
  fun `gana el nombre mas especifico`() {
    assertEquals(listOf("glucemia_posprandial"), ids(listOf("GLUCEMIA POST PRANDIAL")))
    assertEquals(listOf("glucemia"), ids(listOf("GLUCEMIA")))
  }
  
  @Test
  fun `si el titulo aparece dos veces vale el primero`() {
    val match = detector.detect(listOf("HEMOGRAMA", "otra linea", "HEMOGRAMA (AA)")).single()
    assertEquals(0, match.lineIndex)
  }
  
  @Test
  fun `el texto original de la linea se conserva`() {
    val match = detector.detect(listOf("HEPA TOGRAMA (AA)")).single()
    assertEquals("HEPA TOGRAMA (AA)", match.text)
  }
  
  @Test
  fun `si no se reconoce nada no se inventa un estudio`() {
    assertNull(detector.detect(listOf("PRACTICAS SU", "OBSERVA CIONES")).firstOrNull())
  }
}