package com.andrespar2.wallance.nucleo.comun

import java.time.LocalDate
import java.time.YearMonth

/**
 * Intervalos de tiempo disponibles para los reportes de Wallance.
 */
enum class RangoPeriodo(val etiqueta: String) {
    HOY("Hoy"),
    SIETE_DIAS("Últimos 7 días"),
    MES_ACTUAL("Mes actual"),
    MES_ANTERIOR("Mes anterior"),
    ANIO_ACTUAL("Año actual"),
    PERSONALIZADO("Personalizado")
}

/** Intervalo concreto calculado a partir de [RangoPeriodo]. */
data class IntervaloFechas(
    val inicio: Long,
    val fin: Long,
    val etiqueta: String
)

object CalculoPeriodos {

    /** Calcula el intervalo real de un periodo para la fecha dada. */
    fun intervalo(periodo: RangoPeriodo, hoy: LocalDate = LocalDate.now()): IntervaloFechas {
        return when (periodo) {
            RangoPeriodo.HOY -> {
                val inicio = Utilidades.milisDeFecha(hoy)
                val fin = Utilidades.milisDeFecha(hoy.plusDays(1)) - 1
                IntervaloFechas(inicio, fin, "Hoy")
            }
            RangoPeriodo.SIETE_DIAS -> {
                val inicio = Utilidades.milisDeFecha(hoy.minusDays(6))
                val fin = Utilidades.milisDeFecha(hoy.plusDays(1)) - 1
                IntervaloFechas(inicio, fin, "Últimos 7 días")
            }
            RangoPeriodo.MES_ACTUAL -> {
                val mes = YearMonth.from(hoy)
                val inicio = Utilidades.milisDeFecha(mes.atDay(1))
                val fin = Utilidades.milisDeFecha(mes.atEndOfMonth()) + 86_399_999
                IntervaloFechas(inicio, fin, mes.humanizar())
            }
            RangoPeriodo.MES_ANTERIOR -> {
                val mes = YearMonth.from(hoy).minusMonths(1)
                val inicio = Utilidades.milisDeFecha(mes.atDay(1))
                val fin = Utilidades.milisDeFecha(mes.atEndOfMonth()) + 86_399_999
                IntervaloFechas(inicio, fin, mes.humanizar())
            }
            RangoPeriodo.ANIO_ACTUAL -> {
                val inicio = Utilidades.milisDeFecha(LocalDate.of(hoy.year, 1, 1))
                val fin = Utilidades.milisDeFecha(LocalDate.of(hoy.year, 12, 31)) + 86_399_999
                IntervaloFechas(inicio, fin, "Año ${hoy.year}")
            }
            RangoPeriodo.PERSONALIZADO -> {
                val inicio = Utilidades.milisDeFecha(hoy.minusDays(30))
                val fin = Utilidades.milisDeFecha(hoy.plusDays(1)) - 1
                IntervaloFechas(inicio, fin, "Últimos 30 días")
            }
        }
    }

    /** Devuelve los últimos [cantidad] meses cerrados con su etiqueta. */
    fun ultimosMeses(cantidad: Int, hoy: LocalDate = LocalDate.now()): List<YearMonth> =
        (0 until cantidad).map { YearMonth.from(hoy).minusMonths((cantidad - 1 - it).toLong()) }

    private fun YearMonth.humanizar(): String {
        val nombre = when (monthValue) {
            1 -> "enero"; 2 -> "febrero"; 3 -> "marzo"; 4 -> "abril"
            5 -> "mayo"; 6 -> "junio"; 7 -> "julio"; 8 -> "agosto"
            9 -> "septiembre"; 10 -> "octubre"; 11 -> "noviembre"; 12 -> "diciembre"
            else -> ""
        }
        return "$nombre de $year"
    }
}
