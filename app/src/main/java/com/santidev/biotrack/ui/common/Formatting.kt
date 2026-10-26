package com.santidev.biotrack.ui.common

import androidx.compose.ui.graphics.Color
import com.santidev.biotrack.data.model.Measurement
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

val numberFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale("es", "AR")))
fun formatNumber(value: Double): String = numberFormat.format(value)
fun Measurement.displayValue(): String = valueText.ifBlank { value?.let { formatNumber(it) } ?: "" }

val UpColor = Color(0xFF58E600)// verde: subió
val DownColor = Color(0xFFF44336) //rojo: Bajo
val EqualColor = Color(0xFF1157CF) // azul: sigue igual