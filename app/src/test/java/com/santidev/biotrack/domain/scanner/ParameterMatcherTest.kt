package com.santidev.biotrack.domain.scanner

import com.santidev.biotrack.domain.catalog.CatalogParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParameterMatcherTest {
  
  private fun p(id: String, name: String, vararg aliases: String) =
    CatalogParameter(id = id, name = name, aliases = aliases.toList())
  
  // Como los escribiría quien arma el catálogo
  private val matcher = ParameterMatcher(
    listOf(
      p("globulos_rojos", "Glóbulos rojos", "eritrocitos", "hematies", "rbc"),
      p("hematocrito", "Hematocrito", "hto", "hct"),
      p("hemoglobina", "Hemoglobina", "hb", "hgb"),
      p("vcm", "Volumen corpuscular medio", "vcm", "mcv"),
      p("hcm", "Hemoglobina corpuscular media", "hcm", "mch"),
      p("chcm", "Concentración de hemoglobina corpuscular media", "chcm", "mchc", "cc hemoglob corpusc media"),
      p("rdw", "Ancho de distribución eritrocitaria", "rdw", "ade", "indice distrib eritroc"),
      p("mpv", "Volumen plaquetario medio", "mpv", "vpm"),
      p("pct", "Plaquetocrito", "pct"),
      p("pdw", "Índice de distribución plaquetaria", "pdw", "indice distrib plaquet"),
      p("leucocitos", "Glóbulos blancos", "leucocitos", "wbc"),
      p("cayados", "Neutrófilos en cayado", "cayados", "baciliformes"),
      p("segmentados", "Neutrófilos segmentados"),
      p("eosinofilos", "Eosinófilos"),
      p("basofilos", "Basófilos"),
      p("linfocitos", "Linfocitos"),
      p("monocitos", "Monocitos"),
      p("bil_total", "Bilirrubina total"),
      p("bil_directa", "Bilirrubina directa"),
      p("bil_indirecta", "Bilirrubina indirecta"),
      p("tgo", "TGO", "ast", "transaminasa glutamico oxalacetica"),
      p("tgp", "TGP", "alt", "transaminasa glutamico piruvica"),
      p("fa", "Fosfatasa alcalina", "fal"),
      p("col_total", "Colesterol total", "colesterol"),
      p("hdl", "HDL colesterol", "hdl"),
      p("castelli", "Factor de riesgo coronario", "castelli"),
      p("ldl", "LDL colesterol", "ldl"),
      p("trigliceridos", "Triglicéridos"),
      p("ferremia", "Ferremia", "hierro serico", "sideremia"),
      p("glucemia", "Glucemia", "glucosa"),
      p("uremia", "Uremia", "urea")
    )
  )
  
  private fun idOf(text: String) = matcher.match(text)?.parameter?.id
  
  @Test
  fun `los nombres de un hemograma real se reconocen`() {
    val expected = mapOf(
      "RECUENTO DE GLOBULOS ROJOS" to "globulos_rojos",
      "HEMATOCRITO" to "hematocrito",
      "HEMOGLOBINA" to "hemoglobina",
      "VOLUMEN CORPUS CULAR MEDIO (VCM)" to "vcm",
      "HEMOGLOB.CORPUSCULAR MEDIA(MCH)-HCM" to "hcm",
      "CC.HEMOGLOB. CORPUSC. MEDIA (MCHC)-CHCM" to "chcm",
      "INDICE DISTRIB. ERITROC.(RDW)" to "rdw",
      "VOLUMEN PLAQUETARIO MEDIO (MPV)" to "mpv",
      "PLAQUETOCRITO (PCT)" to "pct",
      "INDICE DISTRIB. PLAQUET.(PDW)" to "pdw",
      "RECUENTO DE LEUCOCITOS" to "leucocitos",
      "NEUTROFILOS EN CAYADO" to "cayados",
      "NEUTROFILOS SEGMENTADOS" to "segmentados",
      "EOSINOFILOS" to "eosinofilos",
      "BASOFILOS" to "basofilos",
      "LINFOCITOS" to "linfocitos",
      "MONOCITOS" to "monocitos"
    )
    expected.forEach { (line, id) -> assertEquals("Línea: $line", id, idOf(line)) }
  }
  
  @Test
  fun `los nombres de un hepatograma y de estudios de un solo parametro se reconocen`() {
    val expected = mapOf(
      "BILIRRUBINA TOTAL" to "bil_total",
      "BIL IRRUBINA DIRECTA" to "bil_directa",
      "BILIRRUBINA INDIRECTA" to "bil_indirecta",
      "TGO-AST-TRANSAMIN.GLUTAMICO-OXALACETICA" to "tgo",
      "TGP-ALT -TRANSAMIN.GLUTAMICO-PIRUVICA" to "tgp",
      "FOSFATASA ALCALINA" to "fa",
      "COLESTEROL TOTAL" to "col_total",
      "HDL-COLESTEROL" to "hdl",
      "FACTOR DE RIESGO CORONARIO (CASTELLI)" to "castelli",
      "LDL-COLESTEROL" to "ldl",
      "TRIG IOERIDOS" to "trigliceridos",
      "FERREMIA" to "ferremia",
      "GLUCEMIA" to "glucemia",
      "UREMIA" to "uremia"
    )
    expected.forEach { (line, id) -> assertEquals("Línea: $line", id, idOf(line)) }
  }
  
  @Test
  fun `se informa que tan segura es la coincidencia`() {
    assertEquals(Confidence.EXACT, matcher.match("HEMATOCRITO")?.confidence)
    assertEquals(Confidence.PREFIX, matcher.match("VOLUMEN CORPUS CULAR MEDIO (VCM)")?.confidence)
    assertEquals(Confidence.FUZZY, matcher.match("HEMATOCRTO")?.confidence)
    assertEquals(Confidence.FUZZY, matcher.match("TRIG IOERIDOS")?.confidence)
  }
  
  @Test
  fun `el texto original se conserva`() {
    assertEquals("BIL IRRUBINA DIRECTA", matcher.match("BIL IRRUBINA DIRECTA")?.text)
  }
  
  @Test
  fun `lo que no es un nombre no se identifica`() {
    listOf(
      "METODO:TRINDER - ARCHITECT C8000/4000- TRANSFERENCIA AUTOMATICA DE RESULTADOS POR INTERFAZ (AA)",
      "METODO:UREASA, UV - ARCHITECT C8000/4000- TRANSFERENCIA AUTOMATICA DE RESULTADOS POR INTERFAZ (AA)",
      "SERTE ROJA: Recuento e indices hematimetricos",
      "OBSERVA CIONES",
      "de la prestacion.",
      ""
    ).forEach { assertNull("Línea: $it", idOf(it)) }
  }
  
  @Test
  fun `un nombre cortado por el papel se reconoce si es unico`() {
    val cut = ParameterMatcher(
      listOf(
        p("chcm", "Concentración hemoglobina corpuscular media"),
        p("hcm", "Hemoglobina corpuscular media")
      )
    )
    val match = cut.match("CONCENTRACION HEMOGLOBINA CORP")
    assertEquals("chcm", match?.parameter?.id)
    assertEquals(Confidence.PREFIX, match?.confidence)
  }
  
  @Test
  fun `si las partes apuntan a parametros distintos no se identifica`() {
    val ambiguous = ParameterMatcher(listOf(p("hdl", "HDL"), p("col", "Colesterol")))
    assertNull(ambiguous.match("HDL-COLESTEROL"))
  }
  
  @Test
  fun `un nombre parecido pero distinto no se confunde`() {
    // "indirecta" no es una letra mal leída de "directa"
    assertNull(ParameterMatcher(listOf(p("bil_directa", "Bilirrubina directa"))).match("BILIRRUBINA INDIRECTA"))
    // T3 no es T4
    assertNull(ParameterMatcher(listOf(p("t4", "T4 libre"))).match("T3 LIBRE"))
  }
}