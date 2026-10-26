package com.santidev.biotrack.ui.common

import java.util.Calendar
import java.util.TimeZone


//El selector de fecha trabaja en UTC a medianoche.
//Guardamos la fecha al mediodía de la zona local, para que nunca se corra un día al mostrarla.

internal fun todayAtNoon(): Long = Calendar.getInstance().apply {
  set(Calendar.HOUR_OF_DAY, 12)
  set(Calendar.MINUTE, 0)
  set(Calendar.SECOND, 0)
  set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun utcMidnightToLocalNoon(utcMillis: Long): Long {
  val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
  return Calendar.getInstance().apply {
    clear()
    set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
  }.timeInMillis
}

fun localMillisToUtcMidnight(localMillis: Long): Long {
  val local = Calendar.getInstance().apply { timeInMillis = localMillis }
  return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    clear()
    set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
  }.timeInMillis
}