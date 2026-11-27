package com.santidev.biotrack.domain

import com.santidev.biotrack.model.Lab
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class CompareTest {
  
  private fun glucemia(date: Long, value: String) = lab(date, labResult(value = value))
  
  //compara el laboratorio mas nuevo contra los anteriores
  private fun compareNewest(vararg labs: Lab): Comparison? {
    val newest = labs.maxBy { it.date }
    return compare(labs.toList(), newest, newest.results.first())
  }
  
  @Test
  fun `un valor que sube se compara con el laboratorio anterior`() {
    val c = compareNewest(glucemia(1, "92"), glucemia(2, "105"))!!
    assertEquals(Trend.UP, c.trend)
    assertEquals("13 mg/dL", c.difference)
    assertEquals(1L, c.previousDate)
    assertEquals("92", c.previous.value)
  }
  
  @Test
  fun `un valor que baja`() {
    val c = compareNewest(glucemia(1, "105"), glucemia(2, "92"))!!
    assertEquals(Trend.DOWN, c.trend)
    assertEquals("13 mg/dL", c.difference)
  }
  
  @Test
  fun `un valor igual es SAME`() {
    val c = compareNewest(glucemia(1, "92"), glucemia(2, "92"))!!
    assertEquals(Trend.SAME, c.trend)
    assertEquals("0 mg/dL", c.difference)
  }
  
  @Test
  fun `la diferencia se muestra con coma decimal`() {
    val c = compareNewest(glucemia(1, "10,5"), glucemia(2, "8,2"))!!
    assertEquals("2,3 mg/dL", c.difference)
  }
  
  @Test
  fun `el punto de miles se interpreta antes de restar`() {
    val old = lab(1, labResult(name = "Leucocitos", value = "7.629", unit = "/mm³"))
    val new = lab(2, labResult(name = "Leucocitos", value = "8.100", unit = "/mm³"))
    assertEquals("471 /mm³", compareNewest(old, new)!!.difference)
  }
  
  @Test
  fun `porcentaje y unidad vacia`() {
    val pctOld = lab(1, labResult(name = "Hematocrito", value = "42", unit = "%"))
    val pctNew = lab(2, labResult(name = "Hematocrito", value = "44,1", unit = "%"))
    assertEquals("2,1 %", compareNewest(pctOld, pctNew)!!.difference)
    
    val phOld = lab(1, labResult(name = "pH", value = "5,5", unit = ""))
    val phNew = lab(2, labResult(name = "pH", value = "6", unit = ""))
    assertEquals("0,5", compareNewest(phOld, phNew)!!.difference)
  }
  
  @Test
  fun `sin laboratorio anterior no hay comparacion`() {
    assertNull(compareNewest(glucemia(1, "92")))
  }
  
  @Test
  fun `solo se compara con laboratorios mas viejos, no con posteriores ni de la misma fecha`() {
    val l1 = glucemia(1, "92")
    val l2 = glucemia(2, "100")
    val sameDate = glucemia(2, "999")
    val l3 = glucemia(3, "200")
    val c = compare(listOf(l1, l2, sameDate, l3), l2, l2.results[0])!!
    assertEquals(1L, c.previousDate)
    assertEquals("8 mg/dL", c.difference)
  }
  
  @Test
  fun `se toma el laboratorio anterior mas cercano`() {
    val c = compareNewest(glucemia(1, "50"), glucemia(2, "90"), glucemia(3, "100"))!!
    assertEquals(2L, c.previousDate)
    assertEquals("10 mg/dL", c.difference)
  }
  
  @Test
  fun `se saltan los laboratorios que no tienen ese estudio`() {
    val c = compareNewest(
      glucemia(1, "92"),
      lab(2, labResult(name = "Urea", value = "30")),
      glucemia(3, "105")
    )!!
    assertEquals(1L, c.previousDate)
  }
  
  @Test
  fun `si el anterior no es numerico se usa el siguiente anterior que si lo sea`() {
    val c = compareNewest(glucemia(1, "92"), glucemia(2, "no dosado"), glucemia(3, "105"))!!
    assertEquals(1L, c.previousDate)
    assertEquals("13 mg/dL", c.difference)
  }
  
  @Test
  fun `nombre y unidad no distinguen mayusculas ni espacios`() {
    val old = lab(1, labResult(name = " glucemia ", value = "92", unit = "MG/DL"))
    val new = glucemia(2, "105")
    assertEquals(Trend.UP, compareNewest(old, new)!!.trend)
  }
  
  @Test
  fun `otra unidad u otro nombre no se compara`() {
    assertNull(compareNewest(lab(1, labResult(value = "5", unit = "g/L")), glucemia(2, "92")))
    assertNull(compareNewest(lab(1, labResult(name = "Urea", value = "30")), glucemia(2, "92")))
  }
  
  @Test
  fun `si el resultado no es un numero puro no se compara`() {
    assertNull(compareNewest(glucemia(1, "0,3"), glucemia(2, "<0,5")))
    assertNull(compareNewest(glucemia(1, "0,3"), glucemia(2, "NEGATIVO")))
  }
  
  // ---- tiempos ----
  
  private fun tiempo(date: Long, minutes: Int? = null, seconds: Int? = null) =
    lab(date, labResult(name = "Tiempo de coagulacion", unit = "", minutes = minutes, seconds = seconds))
  
  @Test
  fun `los tiempos se comparan en segundos y se muestran en min y seg`() {
    val c = compareNewest(tiempo(1, minutes = 5), tiempo(2, minutes = 7, seconds = 30))!!
    assertEquals(Trend.UP, c.trend)
    assertEquals("2 min 30 seg", c.difference)
  }
  
  @Test
  fun `un tiempo que baja a menos de un minuto se muestra en segundos`() {
    val c = compareNewest(tiempo(1, minutes = 1, seconds = 30), tiempo(2, seconds = 45))!!
    assertEquals(Trend.DOWN, c.trend)
    assertEquals("45 seg", c.difference)
  }
  
  @Test
  fun `un tiempo y un numero con el mismo nombre no se comparan`() {
    val old = lab(1, labResult(name = "Tiempo de coagulacion", value = "7", unit = ""))
    assertNull(compareNewest(old, tiempo(2, minutes = 7)))
  }
  
  // HOY FALLA: muestra "2 min 0 seg" (el arreglo esta en la seccion 6)
  @Test
  fun `una diferencia de minutos exactos no muestra 0 seg`() {
    val c = compareNewest(tiempo(1, minutes = 5), tiempo(2, minutes = 7))!!
    assertEquals("2 min", c.difference)
  }
}