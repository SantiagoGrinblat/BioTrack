package com.santidev.biotrack.data.catalog

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.FileNotFoundException

class CatalogRepositoryTest {
  
  private val files = mapOf(
    "catalog.json" to """
      {"estudios": [
        {"id": "hemograma", "nombre": "Hemograma", "sinonimos": [], "archivo": "estudios/hemograma.json"},
        {"id": "hepatograma", "nombre": "Hepatograma", "sinonimos": [], "archivo": "estudios/hepatograma.json"},
        {"id": "roto", "nombre": "Roto", "sinonimos": [], "archivo": "estudios/roto.json"},
        {"id": "falta", "nombre": "Falta", "sinonimos": [], "archivo": "estudios/falta.json"}
      ]}
    """,
    // con secciones
    "estudios/hemograma.json" to """
      {"id": "hemograma", "nombre": "Hemograma", "secciones": [
        {"id": "serie_roja", "nombre": "Serie roja", "parametros": [
          {"id": "hemograma.hemoglobina", "nombre": "Hemoglobina", "sinonimos": ["hb"], "unidades": ["g/dl"]}
        ]}
      ]}
    """,
    // sin secciones, y con un campo que la app no conoce
    "estudios/hepatograma.json" to """
      {"id": "hepatograma", "nombre": "Hepatograma", "comentario": "se ignora",
       "parametros": [{"id": "hepatograma.tgo", "nombre": "TGO"}]}
    """,
    "estudios/roto.json" to """{"id": "roto", "nombre": """
  )
  
  private val reads = mutableListOf<String>()
  private val errors = mutableListOf<String>()
  
  private fun repository() = CatalogRepository(
    readAsset = { path ->
      reads += path
      files[path] ?: throw FileNotFoundException(path)
    },
    logError = { message, _ -> errors += message }
  )
  
  @Test
  fun `leer el indice no lee ningun estudio`() = runBlocking {
    val repository = repository()
    assertEquals(4, repository.studies().size)
    assertEquals(listOf("catalog.json"), reads)
    assertEquals(0, repository.loadedStudyCount)
  }
  
  @Test
  fun `cargar un estudio lee solo su archivo y despues sale de memoria`() = runBlocking {
    val repository = repository()
    val hemograma = repository.studies().first { it.id == "hemograma" }
    
    repository.study(hemograma)
    repository.study(hemograma)
    
    assertEquals(listOf("catalog.json", "estudios/hemograma.json"), reads)
    assertEquals(1, repository.loadedStudyCount)
  }
  
  @Test
  fun `los parametros salen tanto de secciones como sueltos`() = runBlocking {
    val repository = repository()
    val entries = repository.studies()
    
    val hemograma = repository.study(entries.first { it.id == "hemograma" })!!
    val hepatograma = repository.study(entries.first { it.id == "hepatograma" })!!
    
    assertEquals(listOf("hemograma.hemoglobina"), hemograma.parameters.map { it.id })
    assertEquals(listOf("hepatograma.tgo"), hepatograma.parameters.map { it.id })
    assertEquals("numero", hepatograma.parameters.single().type)   // valor por defecto
  }
  
  @Test
  fun `un archivo roto o ausente devuelve null y se informa`() = runBlocking {
    val repository = repository()
    val entries = repository.studies()
    
    assertNull(repository.study(entries.first { it.id == "roto" }))
    assertNull(repository.study(entries.first { it.id == "falta" }))
    assertEquals(2, errors.size)
    assertEquals(0, repository.loadedStudyCount)
  }
}