package com.santidev.biotrack.domain.scanner

//una linea de texto reconocida y el rectangulo donde estaba en la imagen (en pixeles)
data class OcrLine(
  val text: String,
  val left: Int,
  val top: Int,
  val right: Int,
  val bottom: Int
)

//lo que se reconocio en una imagen
data class OcrPage(
  val width: Int,
  val height: Int,
  val lines: List<OcrLine>
)
