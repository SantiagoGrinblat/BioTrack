package com.santidev.biotrack.data.catalog

import android.content.Context
import android.util.Log
import com.santidev.biotrack.domain.catalog.CatalogStudy
import com.santidev.biotrack.domain.catalog.StudyEntry
import com.santidev.biotrack.domain.catalog.decodeCatalogIndex
import com.santidev.biotrack.domain.catalog.decodeCatalogStudy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

//esto lee el catalogo de los assets. solo conoce archivos y JSON: no hace OCR ni busca nada en los informes.
//! - El indice (catalog.json) se lee una vez.
//! - Cada estudio se lee solo cuando se pide, y despues queda en la memoria.
//readAsset recibe la ruta dentro de assets y devuelve el texto del archivo.
class CatalogRepository(
  private val readAsset: (path: String) -> String,
  private val logError: (message: String, error: Throwable) -> Unit = { _, _ -> }
) {
  
  private val lock = Mutex()
  private var index: List<StudyEntry>? = null
  private val loaded = mutableMapOf<String, CatalogStudy>()
  
  //cuantos estudios se leyeron hasta ahora (para comprobar que no se cargan de mas)
  val loadedStudyCount: Int get() = loaded.size
  
  //los estudios que existen: solo nombres y archivos, sin parametros
  suspend fun studies(): List<StudyEntry> = lock.withLock {
    index?.let { return@withLock it }
    val read = withContext(Dispatchers.IO) { readIndex() }
    if (read != null) index = read
    read.orEmpty()
  }
  
  //el estudio completo, con sus parametros. devuelve null si el archivo falta O si esta mal escrito.
  suspend fun study(entry: StudyEntry): CatalogStudy? = lock.withLock {
    loaded[entry.id]?.let { return@withLock it }
    val read = withContext(Dispatchers.IO) { readStudy(entry) }
    if (read != null) loaded[entry.id] = read
    read
  }
  
  private fun readIndex(): List<StudyEntry>? =
    try {
      decodeCatalogIndex(readAsset(INDEX_FILE)).studies
    } catch (error: Exception) {
      logError("No se pudo leer $INDEX_FILE", error)
      null
    }
  
  private fun readStudy(entry: StudyEntry): CatalogStudy? =
    try {
      decodeCatalogStudy(readAsset(entry.file))
    } catch (e: Exception) {
      //el mensaje de la excepcion indica en que parte del JSON esta el error
      logError("No se pudo leer ${entry.file}", e)
      null
    }
  
  companion object {
    private const val TAG = "CatalogRepository"
    private const val INDEX_FILE = "catalog.json"
    
    fun fromAssets(context: Context) = CatalogRepository(
      readAsset = { path ->
        context.applicationContext.assets.open(path).bufferedReader().use { it.readText() }
      },
      logError = { message, error -> Log.e(TAG, message, error) }
    )
  }
}