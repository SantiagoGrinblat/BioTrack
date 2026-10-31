package com.santidev.biotrack.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// El selector de fecha trabaja en UTC a medianoche. Se guarda ese mismo valor
// y se muestra en UTC, así la fecha nunca se corre un día.

fun todayUtcMillis(): Long {
  val local = Calendar.getInstance()
  return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    clear()
    set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
  }.timeInMillis
}

fun formatDate(utcMillis: Long): String {
  val format = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "AR"))
  format.timeZone = TimeZone.getTimeZone("UTC")
  return format.format(Date(utcMillis))
}