package com.santidev.biotrack.domain

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ParseNumberTest {
  
  private fun assertCases(tiposDeCasos: Map<String, Double>) {
    tiposDeCasos.forEach { (input, esperado) ->
      val resultado = parseNumber(input)
      assertEquals("fallo en el input \"$input\"", esperado, resultado)
    }
  }
  
  @Test
  fun `El punto seguido de 1 o 2 digitos se interpreta como decimal`() {
    assertCases(
      mapOf(
        "7.5" to 7.5,
        "25.51" to 25.51,
        "100.0" to 100.0,
        "1.2345" to 1.2345,
      )
    )
  }
  
  @ParameterizedTest(name = "[{index}] {0} -> {1}")
  @CsvSource(
    "7.5, 7.5",
    "25.51, 25.51",
    "100.0, 100.0",
    "1.2345, 1.2345"
  )
  fun `el punto seguido de digitos se interpreta como decimal`(
    input: String,
    expected: Double
  ) {
    assertEquals(expected, parseNumber(input)!!, 0.000000001)
  }
  
  @Test
  fun `La coma seguida de uno o mas digitos se interpreta como decimal`() {
    assertCases(
      mapOf(
        "2,35" to 2.35,
        "17,51" to 17.51,
        "113,2" to 113.2,
        "6.100,25" to 6100.25,
      )
    )
  }
  
  @Test
  fun `El punto seguido de tres digitos se interpreta como miles`() {
    assertCases(
      mapOf(
        "8.680" to 8680.0,
        "12.345" to 12345.0,
        "161.000" to 161000.0,
        "5.150.000" to 5150000.0,
      )
    )
  }
  
  @Test
  fun `Los ceros al final de los decimales con coma no cambian el valor total`() {
    assertCases(
      mapOf(
        "10,50" to 10.5,
        "100,0" to 100.0,
        "1,050" to 1.05,
        "1,500" to 1.5,
      )
    )
  }
  
  @Test
  fun `Los ceros en el medio de los decimales con coma si cuentan en el valor total`() {
    assertCases(
      mapOf(
        "1,01" to 1.01,
        "1,005" to 1.005,
        "10,0023" to 10.0023,
      )
    )
  }
  
  @Test
  fun `Un cero inicial nunca es separador de miles`() {
    assertCases(
      mapOf(
        "0.1" to 0.1,
        "0.43" to 0.43,
        "0.04" to 0.04,
        "0.20" to 0.2,
        "0.800" to 0.8,
        "0.123" to 0.123,
        "0,1" to 0.1,
        "0,43" to 0.43,
        "0,20" to 0.2,
        "0,500" to 0.5,
      )
    )
  }
  
  @Test
  fun `Los numeros negativos conservan el signo en todos los tipos de formatos`() {
    assertCases(
      mapOf(
        "-0,5" to -0.5,
        "-0.123" to -0.123,
        "-4.5" to -4.5,
        "-11,9" to -11.9,
        "-6.220" to -6220.0,
        "-1.753.999,25" to -1753999.25,
      )
    )
  }
  
  @Test
  fun `Los numeros enteros sin separador se interpretan tal cual`() {
    assertCases(
      mapOf(
        "2" to 2.0,
        "64" to 64.0,
        "145" to 145.0,
      )
    )
  }
  
  @Test
  fun `Los espacios en blanco antes y despues del numero se ignoran`() {
    assertEquals(6500.0, parseNumber("  6.500  "))
  }
  
  @Test
  fun `El texto que no es un numero valido devuelve null`() {
    listOf("abc", "", " ", "1.23.456", "01.234.566", "3,2,1", "1b.20","-", ".5", ",5", "1.", "1,", "--5","+5", "1.234,", "5 %", "<0,5", "1.2.3", "1,5.5" ).forEach { input ->
      assertNull("deberia ser null con \"$input\"", parseNumber(input))
    }
  }
  
  @Test
  fun `El punto seguido de cuatro o mas digitos es decimal porque no puede ser miles`() {
    assertCases(
      mapOf(
        "1.2345" to 1.2345,
        "0.12345" to 0.12345,
      )
    )
  }
  
  @Test
  fun `punto y tres digitos son miles aunque el laboratorio quiera decir decimal`() {
    assertCases(
      mapOf(
        "12.345" to 12345.0,
        "1.945" to 1945.0,
        "1.010" to 1010.0,
      )
    )
  }
  
  @Test
  fun `Los negativos con cero inicial conservan el signo`() {
    assertCases(
      mapOf(
        "-0,5" to -0.5,
        "-0.123" to -0.123,
      )
    )
  }
  
}