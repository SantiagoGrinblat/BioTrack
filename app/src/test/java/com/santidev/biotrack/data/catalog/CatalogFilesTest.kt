package com.santidev.biotrack.data.catalog

import com.santidev.biotrack.domain.catalog.StudyEntry
import com.santidev.biotrack.domain.catalog.decodeCatalogIndex
import com.santidev.biotrack.domain.catalog.decodeCatalogStudy
import com.santidev.biotrack.domain.scanner.UnitCatalog
import com.santidev.biotrack.domain.scanner.normalize
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Revisa los JSON REALES de app/src/main/assets. Conviene correrlo cada vez que se editan.
class CatalogFilesTest {
  
  private val assets: File =
    listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }
  
  private val index = decodeCatalogIndex(File(assets, "catalog.json").readText())
  
  private val existing = index.studies.filter { File(assets, it.file).exists() }
  
  private fun load(entry: StudyEntry) = decodeCatalogStudy(File(assets, entry.file).readText())
  
  @Test
  fun `cada estudio del indice tiene id nombre y archivo`() {
    val incomplete = index.studies.filter { it.id.isBlank() || it.name.isBlank() || it.file.isBlank() }
    assertTrue("Incompletos: $incomplete", incomplete.isEmpty())
  }
  
  @Test
  fun `los ids del indice no se repiten`() {
    val repeated = index.studies.groupBy { it.id }.filter { it.value.size > 1 }.keys
    assertTrue("Ids repetidos en catalog.json: $repeated", repeated.isEmpty())
  }
  
  @Test
  fun `todos los archivos de estudios que existen se leen sin errores`() {
    val failures = existing.mapNotNull { entry ->
      runCatching { load(entry) }.exceptionOrNull()?.let { "${entry.file}: ${it.message}" }
    }
    assertTrue("JSON con errores:\n" + failures.joinToString("\n"), failures.isEmpty())
  }
  
  @Test
  fun `cada estudio tiene el mismo id que su entrada del indice`() {
    val different = existing.filter { load(it).id != it.id }.map { it.file }
    assertTrue("El id del archivo no coincide con catalog.json: $different", different.isEmpty())
  }
  
  @Test
  fun `los ids de parametros no se repiten`() {
    val ids = existing.flatMap { load(it).parameters }.map { it.id }
    val repeated = ids.groupBy { it }.filter { it.value.size > 1 }.keys
    assertTrue("Ids de parametros repetidos: $repeated", repeated.isEmpty())
  }
  
  @Test
  fun `todos los parametros tienen id y nombre`() {
    val incomplete = existing.flatMap { load(it).parameters }.filter { it.id.isBlank() || it.name.isBlank() }
    assertTrue("Parametros incompletos: $incomplete", incomplete.isEmpty())
  }
  
  // No falla: solo avisa. Un sinonimo repetido entre dos parametros del mismo estudio es ambiguo.
  @Test
  fun `avisos de sinonimos repetidos y archivos pendientes`() {
    val pending = index.studies.filterNot { File(assets, it.file).exists() }.map { it.id }
    println("Estudios del indice sin archivo todavia (${pending.size}): $pending")
    
    val unknownUnits = existing.flatMap { load(it).parameters }
      .flatMap { it.units }.distinct()
      .filter { UnitCatalog.find(it) == null }
    println("Unidades de los JSON que UnitCatalog no conoce: $unknownUnits")
    
    existing.forEach { entry ->
      val owners = mutableMapOf<String, MutableSet<String>>()
      load(entry).parameters.forEach { parameter ->
        (parameter.aliases + parameter.name).map { normalize(it) }.filter { it.isNotEmpty() }
          .forEach { owners.getOrPut(it) { mutableSetOf() }.add(parameter.id) }
      }
      owners.filter { it.value.size > 1 }.forEach { (alias, ids) ->
        println("AVISO ${entry.id}: '$alias' lo usan varios parametros: $ids")
      }
    }
  }
}