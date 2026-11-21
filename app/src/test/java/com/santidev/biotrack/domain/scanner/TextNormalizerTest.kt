package com.santidev.biotrack.domain.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
  
  @Test
  fun `pasa a minusculas y saca las tildes`() {
    assertEquals("hemoglobina", normalize("Hemoglóbina"))
    assertEquals("globulos rojos", normalize("GLÓBULOS ROJOS"))
  }
  
  @Test
  fun `los signos pasan a espacio`() {
    assertEquals("hemoglob corpuscular media mch hcm", normalize("HEMOGLOB.CORPUSCULAR MEDIA(MCH)-HCM"))
  }
  
  @Test
  fun `compact junta las palabras partidas`() {
    assertEquals("hepatogramaaa", compact("HEPA TOGRAMA (AA)"))
  }
  
  @Test
  fun `levenshtein cuenta las letras distintas`() {
    assertEquals(0, levenshtein("hepatograma", "hepatograma"))
    assertEquals(1, levenshtein("hepatoqrama", "hepatograma"))
  }
}