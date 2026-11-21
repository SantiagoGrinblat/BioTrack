package com.santidev.biotrack.domain.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

//Indice = assets/catalog.json
//dice que estudios existen y en que archivo esta cada uno. No trae los parametros.
@Serializable
data class CatalogIndex(
  @SerialName("estudios") val studies: List<StudyEntry> = emptyList()
)

@Serializable
data class StudyEntry(
  val id: String,
  @SerialName("nombre") val name: String,
  @SerialName("sinonimos") val aliases: List<String> = emptyList(),
  @SerialName("archivo") val file: String
  //ruta dentro de assets ej = "estudios/hemograma.json"
)

//estudio completo = assets/estudios/<estudio>.json

@Serializable
data class CatalogStudy(
  val id: String,
  @SerialName("nombre") val name: String,
  @SerialName("sinonimos") val aliases: List<String> = emptyList(),
  @SerialName("secciones") val sections: List<CatalogSection> = emptyList(),
  //para los estudios simples que no usan secciones
  @SerialName("parametros") val ownParameters: List<CatalogParameter> = emptyList()
) {
  //todos los parametros, esten dentro de una seccion O sueltos
  val parameters: List<CatalogParameter>
    get() = ownParameters + sections.flatMap { it.parameters }
}

@Serializable
data class CatalogSection(
  val id: String = "",
  @SerialName("nombre") val name: String = "",
  @SerialName("parametros") val parameters: List<CatalogParameter> = emptyList()
)

@Serializable
data class CatalogParameter(
  val id: String,
  @SerialName("nombre") val name: String,
  @SerialName("sinonimos") val aliases: List<String> = emptyList(),
  @SerialName("unidades") val units: List<String> = emptyList(),
  @SerialName("tipo") val type: String = "numero"
) {
  val isText: Boolean get() = type == "texto"
}

//las claves que no conozca se ignoran, asi se pueden agregar campos a los JSON sin romper nada
private val json = Json { ignoreUnknownKeys = true }

fun decodeCatalogIndex(text: String): CatalogIndex = json.decodeFromString(text)

fun decodeCatalogStudy(text: String): CatalogStudy = json.decodeFromString(text)