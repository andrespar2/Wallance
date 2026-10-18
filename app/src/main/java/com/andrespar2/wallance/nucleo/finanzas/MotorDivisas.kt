package com.andrespar2.wallance.nucleo.finanzas

import com.andrespar2.wallance.nucleo.comun.Utilidades.redondear
import com.andrespar2.wallance.nucleo.comun.Utilidades.redondearMonetario
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Motor de conversión entre monedas.
 *
 * Trabaja exclusivamente con [BigDecimal]. Una tasa es la cantidad de
 * unidades de la moneda destino por una unidad de la moneda base.
 */
object MotorDivisas {

    /** Escala de una tasa de cambio (6 decimales es lo habitual en FX). */
    private const val ESCALA_TASA = 6

    fun normalizarTasa(tasa: BigDecimal): BigDecimal =
        tasa.setScale(ESCALA_TASA, RoundingMode.HALF_UP)

    /**
     * Convierte una cantidad de una moneda a otra usando la tasa directa
     * base→destino. Si la tasa es inversa (destino→base) se divide.
     */
    fun convertir(
        cantidad: BigDecimal,
        tasaDirecta: BigDecimal?,
        tasaInversa: BigDecimal?
    ): BigDecimal? {
        if (tasaDirecta != null && tasaDirecta.signum() > 0) {
            return cantidad.multiply(tasaDirecta).redondearMonetario()
        }
        if (tasaInversa != null && tasaInversa.signum() > 0) {
            return cantidad.divide(tasaInversa, RoundingMode.HALF_UP).redondearMonetario()
        }
        return null
    }

    /**
     * Deriva la tasa entre dos monedas a partir de dos cotizaciones con la
     * misma base: tasa(A→B) = tasa(base→B) / tasa(base→A).
     */
    fun tasaCruzada(tasaBaseDestino: BigDecimal?, tasaBaseOrigen: BigDecimal?): BigDecimal? {
        if (tasaBaseDestino == null || tasaBaseOrigen == null) return null
        if (tasaBaseOrigen.signum() == 0) return null
        return tasaBaseDestino.divide(tasaBaseOrigen, ESCALA_TASA, RoundingMode.HALF_UP)
    }

    /**
     * Calcula la variación porcentual entre dos tasas históricas.
     * Devuelve null si no hay información comparable.
     */
    fun variacionPorcentual(actual: BigDecimal?, anterior: BigDecimal?): BigDecimal? {
        if (actual == null || anterior == null) return null
        if (anterior.signum() == 0) return null
        return actual.subtract(anterior)
            .divide(anterior, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .redondear(2)
    }

    /** Monedas disponibles en Wallance con su nombre completo. */
    val MONEDAS: List<Pair<String, String>> = listOf(
        "COP" to "Peso colombiano",
        "USD" to "Dólar estadounidense",
        "ARS" to "Peso argentino",
        "EUR" to "Euro",
        "MXN" to "Peso mexicano",
        "PEN" to "Sol peruano",
        "CLP" to "Peso chileno",
        "BRL" to "Real brasileño",
        "GBP" to "Libra esterlina",
        "JPY" to "Yen japonés",
        "CAD" to "Dólar canadiense",
        "CHF" to "Franco suizo"
    )

    fun nombreDeMoneda(codigo: String): String =
        MONEDAS.firstOrNull { it.first == codigo }?.second ?: codigo

    /**
     * Parejas de cotización sugeridas para la moneda principal del usuario,
     * en el formato base/cotizada (por ejemplo USD/COP = cuántos COP cuesta 1 USD).
     */
    fun parejasSugeridas(monedaPrincipal: String): List<Pair<String, String>> {
        val contrapartes = when (monedaPrincipal) {
            "COP" -> listOf("USD", "EUR", "ARS")
            "ARS" -> listOf("USD", "EUR", "COP")
            "USD" -> listOf("COP", "ARS", "EUR")
            "EUR" -> listOf("USD", "COP", "GBP")
            else -> listOf("USD", "EUR", "COP")
        }
        return contrapartes.map { par -> par to monedaPrincipal }
    }
}
