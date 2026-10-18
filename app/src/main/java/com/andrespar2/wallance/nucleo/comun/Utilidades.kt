package com.andrespar2.wallance.nucleo.comun

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/**
 * Utilidades monetarias y de fechas de Wallance.
 *
 * Todas las operaciones se realizan con [BigDecimal] usando una escala fija
 * de 2 decimales y redondeo HALF_UP (estándar financiero).
 */
object Utilidades {

    /** Escala monetaria por defecto. */
    const val ESCALA_MONETARIA = 2

    /** Zona horaria del dispositivo. */
    val zona: ZoneId = ZoneId.systemDefault()

    /** Redondea a 2 decimales con redondeo financiero. */
    fun BigDecimal.redondearMonetario(): BigDecimal =
        setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP)

    /** Redondea a la escala indicada. */
    fun BigDecimal.redondear(escala: Int): BigDecimal =
        setScale(escala, RoundingMode.HALF_UP)

    /** Convierte una cantidad textual a BigDecimal o null si no es válida. */
    fun textoADinero(texto: String): BigDecimal? {
        val limpio = texto.trim().replace(" ", "").replace(",", ".")
        if (limpio.isEmpty()) return null
        return try {
            BigDecimal(limpio).redondearMonetario()
        } catch (error: NumberFormatException) {
            null
        }
    }

    /** Formatea una cantidad usando el símbolo de la moneda indicada. */
    fun formatearDinero(cantidad: BigDecimal, codigoMoneda: String = "COP"): String {
        val locale = localeDeMoneda(codigoMoneda)
        val formato = NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        return try {
            formato.currency = Currency.getInstance(codigoMoneda)
            formato.format(cantidad)
        } catch (error: IllegalArgumentException) {
            // Moneda no reconocida: se muestra el código junto a la cantidad.
            "${cantidad.redondearMonetario()} $codigoMoneda"
        }
    }

    /** Devuelve el locale asociado a una moneda, o el de Colombia por defecto. */
    fun localeDeMoneda(codigoMoneda: String): Locale = when (codigoMoneda) {
        "COP" -> Locale("es", "CO")
        "USD" -> Locale("es", "US")
        "ARS" -> Locale("es", "AR")
        "EUR" -> Locale("es", "ES")
        "MXN" -> Locale("es", "MX")
        "PEN" -> Locale("es", "PE")
        "CLP" -> Locale("es", "CL")
        "BRL" -> Locale("pt", "BR")
        "GBP" -> Locale("en", "GB")
        "JPY" -> Locale("ja", "JP")
        "CAD" -> Locale("en", "CA")
        "CHF" -> Locale("de", "CH")
        else -> Locale("es", "CO")
    }

    /** Decimales habituales de una moneda (por defecto 2). */
    fun decimalesDeMoneda(codigoMoneda: String): Int = when (codigoMoneda) {
        "JPY", "CLP" -> 0
        else -> 2
    }

    /** Convierte milisegundos a [LocalDate]. */
    fun fechaDeMilis(milis: Long): LocalDate =
        Instant.ofEpochMilli(milis).atZone(zona).toLocalDate()

    /** Convierte [LocalDate] a milisegundos (inicio del día). */
    fun milisDeFecha(fecha: LocalDate): Long =
        fecha.atStartOfDay(zona).toInstant().toEpochMilli()

    /** Formatea una fecha como "12 de enero de 2026". */
    fun formatearFecha(milis: Long): String {
        val locale = Locale("es", "CO")
        return fechaDeMilis(milis).format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale))
    }

    /** Formatea una fecha corta como "12/01/2026". */
    fun formatearFechaCorta(milis: Long): String =
        fechaDeMilis(milis).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    /** Formatea fecha y hora como "12/01/2026 14:30". */
    fun formatearFechaHora(milis: Long): String {
        val fecha = fechaDeMilis(milis)
        val hora = Instant.ofEpochMilli(milis).atZone(zona).toLocalTime()
        return "${fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} " +
            hora.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    /** Diferencia en días completos entre dos instantes. */
    fun diasEntre(inicio: Long, fin: Long): Long =
        java.time.Duration.between(
            Instant.ofEpochMilli(inicio).atZone(zona),
            Instant.ofEpochMilli(fin).atZone(zona)
        ).toDays()

    /** Devuelve el inicio del día. */
    fun inicioDelDia(milis: Long = System.currentTimeMillis()): Long =
        milisDeFecha(fechaDeMilis(milis))

    /** Suma meses a una fecha conservando el día cuando sea posible. */
    fun masMeses(fecha: LocalDate, meses: Long): LocalDate = fecha.plusMonths(meses)

    /** Suma días a una fecha. */
    fun masDias(fecha: LocalDate, dias: Long): LocalDate = fecha.plusDays(dias)
}
