package com.santidev.biotrack.domain

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class GroupByFolderTest {
  
  private fun item(folder: String, name: String = "Glucemia") =
    SummaryItem(labResult(name = name, folder = folder), 1L, null)
  
  @Test
  fun `Hemograma, hemograma y Hemogramá son la misma carpeta y sin carpeta va al final`() {
    val groups = groupByFolder(
      listOf(item(""), item("Orina"), item("Hemograma"), item("hemogramá "), item("HEMOGRAMA"))
    )
    assertEquals(listOf("Hemograma", "Orina", ""), groups.map { it.name })
    assertEquals(listOf(3, 1, 1), groups.map { it.items.size })
  }
  
  @Test
  fun `el nombre que se muestra es el del primer estudio, sin espacios sobrantes`() {
    val groups = groupByFolder(listOf(item("hemogramá "), item("Hemograma")))
    assertEquals(listOf("hemogramá"), groups.map { it.name })
  }
  
  @Test
  fun `las carpetas se ordenan sin importar tildes ni mayusculas`() {
    val groups = groupByFolder(listOf(item("orina"), item("Ácido úrico"), item("Hemograma")))
    assertEquals(listOf("Ácido úrico", "Hemograma", "orina"), groups.map { it.name })
  }
  
  @Test
  fun `una carpeta solo con espacios cuenta como sin carpeta`() {
    val groups = groupByFolder(listOf(item("   "), item("")))
    assertEquals(1, groups.size)
    assertEquals("", groups.single().name)
    assertEquals(2, groups.single().items.size)
  }
  
  @Test
  fun `dentro de la carpeta se conserva el orden de los estudios`() {
    val groups = groupByFolder(listOf(item("Orina", "Color"), item("Orina", "Aspecto")))
    assertEquals(listOf("Color", "Aspecto"), groups.single().items.map { it.result.name })
  }
  
  @Test
  fun `sin estudios no hay carpetas`() {
    assertTrue(groupByFolder(emptyList()).isEmpty())
  }
}