package com.andrespar2.wallance.nucleo.finanzas

import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.PeriodicidadTasa
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.SistemaAmortizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoInteres
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.Utilidades.redondearMonetario
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

/** Cuota resultante de un cÃ¡lculo de amortizaciÃ³n. */
data class CuotaCalculada(
    val numero: Int,
    val fechaVencimiento: Long,
    val valorTotal: BigDecimal,
    val capital: BigDecimal,
    val interes: BigDecimal
)

/** Resultado completo del cronograma de un prÃ©stamo. */
data class ResumenAmortizacion(
    val cuotas: List<CuotaCalculada>,
    val interesesPrevistos: BigDecimal,
    val totalPrevisto: BigDecimal,
    /** Valor de la cuota constante (o de la primera cuota cuando varÃ­a). */
    val cuotaReferencia: BigDecimal
)

/**
 * Motor de cÃ¡lculo financiero de Wallance.
 *
 * Todas las operaciones se realizan con [BigDecimal] y escala de 2 decimales
 * con redondeo HALF_UP. Nunca se usan tipos de punto flotante.
 *
 * Reglas de precisiÃ³n:
 * - La suma de los capitales de las cuotas es exactamente el capital prestado.
 * - La Ãºltima cuota absorbe cualquier diferencia de redondeo.
 * - Una tasa mensual jamÃ¡s se confunde con una tasa anual: toda tasa lleva
 *   su periodicidad explÃ­cita.
 */
object MotorPrestamos {

    /** Contexto matemÃ¡tico para potencias con exponente negativo. */
    private val contexto = MathContext(40, RoundingMode.HALF_UP)

    /** NÃºmero de periodos de una periodicidad en un aÃ±o. */
    fun periodosAnuales(periodicidad: PeriodicidadTasa): Int = when (periodicidad) {
        PeriodicidadTasa.DIARIA -> 365
        PeriodicidadTasa.SEMANAL -> 52
        PeriodicidadTasa.QUINCENAL -> 24
        PeriodicidadTasa.MENSUAL -> 12
        PeriodicidadTasa.BIMESTRAL -> 6
        PeriodicidadTasa.TRIMESTRAL -> 4
        PeriodicidadTasa.SEMESTRAL -> 2
        PeriodicidadTasa.ANUAL -> 1
    }

    /** NÃºmero de cuotas al aÃ±o para una frecuencia dada. */
    fun cuotasAnuales(frecuencia: FrecuenciaCuota): Int = when (frecuencia) {
        FrecuenciaCuota.UNICA -> 1
        FrecuenciaCuota.SEMANAL -> 52
        FrecuenciaCuota.QUINCENAL -> 24
        FrecuenciaCuota.MENSUAL -> 12
        FrecuenciaCuota.BIMESTRAL -> 6
        FrecuenciaCuota.TRIMESTRAL -> 4
        FrecuenciaCuota.SEMESTRAL -> 2
        FrecuenciaCuota.ANUAL -> 1
    }

    /**
     * Convierte una tasa de una periodicidad a otra.
     *
     * - Si la tasa es nominal, se convierte primero a efectiva para su periodo
     *   (nominal / nÃºmero de periodos del aÃ±o de la tasa).
     * - DespuÃ©s se capitaliza compuestamente hasta la tasa efectiva anual y
     *   desde ahÃ­ se descapitaliza al periodo destino.
     *
     * Ejemplo: 24 % nominal anual = 2 % efectivo mensual.
     */
    fun convertirTasa(
        tasa: BigDecimal,
        de: PeriodicidadTasa,
        a: PeriodicidadTasa,
        nominal: Boolean
    ): BigDecimal {
        if (tasa.signum() == 0) return BigDecimal.ZERO
        if (de == a) return tasa
        val factorDe = BigDecimal(periodosAnuales(de))
        val factorA = BigDecimal(periodosAnuales(a))
        val decimal = tasa.divide(BigDecimal(100), contexto)
        val efectivaDe = if (nominal) decimal.divide(factorDe, contexto) else decimal
        // Tasa efectiva anual.
        val efectivaAnual = BigDecimal.ONE.add(efectivaDe).pow(factorDe.toInt(), contexto)
            .subtract(BigDecimal.ONE)
        // Tasa efectiva del periodo destino.
        val exponente = factorA.divide(factorDe, contexto).toDouble()
        val base = efectivaAnual.add(BigDecimal.ONE).toDouble()
        val periodo = BigDecimal.valueOf(Math.pow(base, exponente)).subtract(BigDecimal.ONE)
        return periodo.multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Devuelve la tasa efectiva por cuota del prÃ©stamo, expresada en decimal
     * (por ejemplo 0.02 para 2 %).
     */
    fun tasaPorCuota(prestamo: Prestamo): BigDecimal {
        if (prestamo.tasaValor.signum() == 0) return BigDecimal.ZERO
        val porcentaje = convertirTasa(
            tasa = prestamo.tasaValor,
            de = prestamo.tasaPeriodicidad,
            a = periodicidadDeFrecuencia(prestamo.frecuenciaCuotas),
            nominal = prestamo.tasaEsNominal
        )
        return porcentaje.divide(BigDecimal(100), contexto)
    }

    /** Periodicidad equivalente a la frecuencia de cuotas. */
    fun periodicidadDeFrecuencia(frecuencia: FrecuenciaCuota): PeriodicidadTasa = when (frecuencia) {
        FrecuenciaCuota.UNICA -> PeriodicidadTasa.MENSUAL
        FrecuenciaCuota.SEMANAL -> PeriodicidadTasa.SEMANAL
        FrecuenciaCuota.QUINCENAL -> PeriodicidadTasa.QUINCENAL
        FrecuenciaCuota.MENSUAL -> PeriodicidadTasa.MENSUAL
        FrecuenciaCuota.BIMESTRAL -> PeriodicidadTasa.BIMESTRAL
        FrecuenciaCuota.TRIMESTRAL -> PeriodicidadTasa.TRIMESTRAL
        FrecuenciaCuota.SEMESTRAL -> PeriodicidadTasa.SEMESTRAL
        FrecuenciaCuota.ANUAL -> PeriodicidadTasa.ANUAL
    }

    /** Suma el nÃºmero de periodos indicado a una fecha segÃºn la frecuencia. */
    fun sumarPeriodos(fecha: LocalDate, frecuencia: FrecuenciaCuota, periodos: Int): LocalDate =
        when (frecuencia) {
            FrecuenciaCuota.UNICA -> fecha
            FrecuenciaCuota.SEMANAL -> fecha.plusDays(7L * periodos)
            FrecuenciaCuota.QUINCENAL -> fecha.plusDays(15L * periodos)
            FrecuenciaCuota.MENSUAL -> fecha.plusMonths(1L * periodos)
            FrecuenciaCuota.BIMESTRAL -> fecha.plusMonths(2L * periodos)
            FrecuenciaCuota.TRIMESTRAL -> fecha.plusMonths(3L * periodos)
            FrecuenciaCuota.SEMESTRAL -> fecha.plusMonths(6L * periodos)
            FrecuenciaCuota.ANUAL -> fecha.plusYears(1L * periodos)
        }

    /**
     * InterÃ©s total previsto para un pago Ãºnico.
     *
     * - Sin interÃ©s: 0.
     * - Porcentaje total / plano: capital Ã— tasa %.
     * - Simple: capital Ã— tasa Ã— nÃºmero de periodos.
     * - Compuesto: capital Ã— ((1 + tasa) ^ n âˆ’ 1).
     */
    fun interesTotalPagoUnico(prestamo: Prestamo): BigDecimal {
        val capital = prestamo.capital
        return when (prestamo.tasaTipo) {
            TipoInteres.SIN_INTERES -> BigDecimal.ZERO
            TipoInteres.PORCENTAJE_TOTAL, TipoInteres.PLANO ->
                capital.multiply(prestamo.tasaValor).divide(BigDecimal(100), contexto)
            TipoInteres.SIMPLE -> {
                val periodos = periodosEntreFechas(prestamo)
                capital.multiply(prestamo.tasaValor).divide(BigDecimal(100), contexto)
                    .multiply(BigDecimal(periodos))
            }
            TipoInteres.COMPUESTO -> {
                val periodos = periodosEntreFechas(prestamo)
                val tasa = prestamo.tasaValor.divide(BigDecimal(100), contexto)
                capital.multiply(BigDecimal.ONE.add(tasa).pow(periodos, contexto))
                    .subtract(capital)
            }
        }.redondearMonetario()
    }

    /**
     * NÃºmero de periodos de interÃ©s entre desembolso y vencimiento segÃºn la
     * periodicidad declarada de la tasa.
     */
    private fun periodosEntreFechas(prestamo: Prestamo): Int {
        val inicio = Utilidades.fechaDeMilis(prestamo.fechaDesembolso)
        val fin = Utilidades.fechaDeMilis(prestamo.fechaVencimiento)
        return when (prestamo.tasaPeriodicidad) {
            PeriodicidadTasa.DIARIA -> inicio.until(fin, java.time.temporal.ChronoUnit.DAYS).toInt()
                .coerceAtLeast(1)
            PeriodicidadTasa.SEMANAL ->
                (inicio.until(fin, java.time.temporal.ChronoUnit.DAYS).toInt() / 7).coerceAtLeast(1)
            PeriodicidadTasa.QUINCENAL ->
                (inicio.until(fin, java.time.temporal.ChronoUnit.DAYS).toInt() / 15).coerceAtLeast(1)
            PeriodicidadTasa.MENSUAL -> inicio.until(fin, java.time.temporal.ChronoUnit.MONTHS).toInt()
                .coerceAtLeast(1)
            PeriodicidadTasa.BIMESTRAL ->
                (inicio.until(fin, java.time.temporal.ChronoUnit.MONTHS).toInt() / 2).coerceAtLeast(1)
            PeriodicidadTasa.TRIMESTRAL ->
                (inicio.until(fin, java.time.temporal.ChronoUnit.MONTHS).toInt() / 3).coerceAtLeast(1)
            PeriodicidadTasa.SEMESTRAL ->
                (inicio.until(fin, java.time.temporal.ChronoUnit.MONTHS).toInt() / 6).coerceAtLeast(1)
            PeriodicidadTasa.ANUAL -> inicio.until(fin, java.time.temporal.ChronoUnit.YEARS).toInt()
                .coerceAtLeast(1)
        }
    }

    /**
     * Calcula el cronograma completo de un prÃ©stamo.
     *
     * @param fechasPersonalizadas fechas de vencimiento cuando el sistema es
     * PERSONALIZADO.
     */
    fun calcularCronograma(
        prestamo: Prestamo,
        fechasPersonalizadas: List<Long> = emptyList()
    ): ResumenAmortizacion {
        val capital = prestamo.capital.redondearMonetario()
        require(capital.signum() >= 0) { "El capital no puede ser negativo." }

        val cuotas = when (prestamo.sistemaAmortizacion) {
            SistemaAmortizacion.PAGO_UNICO -> cuotasPagoUnico(prestamo, capital)
            SistemaAmortizacion.FRANCES -> cuotasFrancesas(prestamo, capital)
            SistemaAmortizacion.ALEMAN -> cuotasAlemanas(prestamo, capital)
            SistemaAmortizacion.INTERESES_PERIODICOS -> cuotasInteresPeriodico(prestamo, capital)
            SistemaAmortizacion.PERSONALIZADO -> cuotasPersonalizadas(prestamo, capital, fechasPersonalizadas)
        }

        val intereses = cuotas.fold(BigDecimal.ZERO) { total, c -> total.add(c.interes) }
            .redondearMonetario()
        val totalPrevisto = cuotas.fold(BigDecimal.ZERO) { total, c -> total.add(c.valorTotal) }
            .redondearMonetario()
        return ResumenAmortizacion(
            cuotas = cuotas,
            interesesPrevistos = intereses,
            totalPrevisto = totalPrevisto,
            cuotaReferencia = cuotas.firstOrNull()?.valorTotal ?: BigDecimal.ZERO
        )
    }

    // ------------------------------------------------------------------
    // Sistemas de amortizaciÃ³n
    // ------------------------------------------------------------------

    private fun cuotasPagoUnico(prestamo: Prestamo, capital: BigDecimal): List<CuotaCalculada> {
        val interes = interesTotalPagoUnico(prestamo)
        return listOf(
            CuotaCalculada(
                numero = 1,
                fechaVencimiento = prestamo.fechaVencimiento,
                valorTotal = capital.add(interes).redondearMonetario(),
                capital = capital,
                interes = interes
            )
        )
    }

    /**
     * Sistema francÃ©s: cuota constante.
     *
     * Cuota = Capital Ã— i / (1 âˆ’ (1 + i)^âˆ’n)
     * Cuando i = 0, la cuota es Capital / n.
     * El interÃ©s de cada cuota se calcula sobre el saldo pendiente y la Ãºltima
     * cuota absorbe la diferencia de redondeo.
     */
    private fun cuotasFrancesas(prestamo: Prestamo, capital: BigDecimal): List<CuotaCalculada> {
        val n = prestamo.numeroCuotas.coerceAtLeast(1)
        val i = tasaPorCuota(prestamo)
        val cuotaConstante = if (i.signum() == 0) {
            capital.divide(BigDecimal(n), contexto)
        } else {
            val factor = BigDecimal.ONE.add(i).pow(-n, contexto)
            capital.multiply(i).divide(BigDecimal.ONE.subtract(factor), contexto)
        }.redondearMonetario()

        val interesEsSimples = prestamo.tasaTipo == TipoInteres.SIMPLE
        val interesPorCuotaFijo = prestamo.tasaTipo == TipoInteres.PLANO ||
            prestamo.tasaTipo == TipoInteres.PORCENTAJE_TOTAL

        return when {
            interesEsSimples -> cuotasInteresFijoSobreCapital(prestamo, capital, n)
            interesPorCuotaFijo -> cuotasInteresPlano(prestamo, capital, n)
            else -> construirCuotasPorSaldo(
                prestamo = prestamo,
                capital = capital,
                n = n,
                tasa = i,
                cuotaConstante = cuotaConstante
            )
        }
    }

    private fun cuotasAlemanas(prestamo: Prestamo, capital: BigDecimal): List<CuotaCalculada> {
        val n = prestamo.numeroCuotas.coerceAtLeast(1)
        return when {
            prestamo.tasaTipo == TipoInteres.SIMPLE ->
                cuotasInteresFijoSobreCapital(prestamo, capital, n)
            prestamo.tasaTipo == TipoInteres.PLANO ||
                prestamo.tasaTipo == TipoInteres.PORCENTAJE_TOTAL ->
                cuotasInteresPlano(prestamo, capital, n)
            else -> {
                val i = tasaPorCuota(prestamo)
                val capitalPorCuota = capital.divide(BigDecimal(n), contexto)
                var saldo = capital
                val cuotas = ArrayList<CuotaCalculada>(n)
                for (numero in 1..n) {
                    val esUltima = numero == n
                    val capitalCuota = if (esUltima) saldo else capitalPorCuota.redondearMonetario()
                    val interesCuota = saldo.multiply(i).redondearMonetario()
                    cuotas.add(
                        CuotaCalculada(
                            numero = numero,
                            fechaVencimiento = fechaDeCuota(prestamo, numero),
                            valorTotal = capitalCuota.add(interesCuota).redondearMonetario(),
                            capital = capitalCuota,
                            interes = interesCuota
                        )
                    )
                    saldo = saldo.subtract(capitalCuota)
                }
                cuotas
            }
        }
    }

    private fun cuotasInteresPeriodico(prestamo: Prestamo, capital: BigDecimal): List<CuotaCalculada> {
        val n = prestamo.numeroCuotas.coerceAtLeast(1)
        return when {
            prestamo.tasaTipo == TipoInteres.SIN_INTERES -> cuotasInteresPlano(prestamo, capital, n)
            prestamo.tasaTipo == TipoInteres.PLANO ||
                prestamo.tasaTipo == TipoInteres.PORCENTAJE_TOTAL ->
                cuotasInteresPlano(prestamo, capital, n)
            prestamo.tasaTipo == TipoInteres.SIMPLE -> {
                val interesPorPeriodo = capital.multiply(prestamo.tasaValor)
                    .divide(BigDecimal(100), contexto).redondearMonetario()
                val cuotas = ArrayList<CuotaCalculada>(n)
                for (numero in 1..n) {
                    val esUltima = numero == n
                    cuotas.add(
                        CuotaCalculada(
                            numero = numero,
                            fechaVencimiento = fechaDeCuota(prestamo, numero),
                            valorTotal = if (esUltima) capital.add(interesPorPeriodo) else interesPorPeriodo,
                            capital = if (esUltima) capital else BigDecimal.ZERO,
                            interes = interesPorPeriodo
                        )
                    )
                }
                cuotas
            }
            else -> {
                val i = tasaPorCuota(prestamo)
                val interesPorPeriodo = capital.multiply(i).redondearMonetario()
                val cuotas = ArrayList<CuotaCalculada>(n)
                for (numero in 1..n) {
                    val esUltima = numero == n
                    cuotas.add(
                        CuotaCalculada(
                            numero = numero,
                            fechaVencimiento = fechaDeCuota(prestamo, numero),
                            valorTotal = if (esUltima) capital.add(interesPorPeriodo) else interesPorPeriodo,
                            capital = if (esUltima) capital else BigDecimal.ZERO,
                            interes = interesPorPeriodo
                        )
                    )
                }
                cuotas
            }
        }
    }

    private fun cuotasPersonalizadas(
        prestamo: Prestamo,
        capital: BigDecimal,
        fechas: List<Long>
    ): List<CuotaCalculada> {
        require(fechas.isNotEmpty()) {
            "El sistema de amortizaciÃ³n personalizado requiere fechas de vencimiento."
        }
        val n = fechas.size
        return when {
            prestamo.tasaTipo == TipoInteres.SIN_INTERES -> {
                val porCuota = repartirCapital(capital, n)
                porCuota.mapIndexed { indice, capitalCuota ->
                    CuotaCalculada(
                        numero = indice + 1,
                        fechaVencimiento = fechas[indice],
                        valorTotal = capitalCuota,
                        capital = capitalCuota,
                        interes = BigDecimal.ZERO
                    )
                }
            }
            prestamo.tasaTipo == TipoInteres.PLANO ||
                prestamo.tasaTipo == TipoInteres.PORCENTAJE_TOTAL -> {
                val interesTotal = capital.multiply(prestamo.tasaValor)
                    .divide(BigDecimal(100), contexto).redondearMonetario()
                combinarCapitalesIntereses(
                    repartirCapital(capital, n),
                    repartirCapital(interesTotal, n),
                    fechas
                )
            }
            else -> cuotasAlemanasPersonalizadas(prestamo, capital, fechas)
        }
    }

    private fun cuotasAlemanasPersonalizadas(
        prestamo: Prestamo,
        capital: BigDecimal,
        fechas: List<Long>
    ): List<CuotaCalculada> {
        val n = fechas.size
        val i = tasaPorCuota(prestamo)
        val capitales = repartirCapital(capital, n)
        var saldo = capital
        return capitales.mapIndexed { indice, capitalCuota ->
            val interesCuota = if (prestamo.tasaTipo == TipoInteres.SIMPLE) {
                capital.multiply(prestamo.tasaValor).divide(BigDecimal(100), contexto)
                    .redondearMonetario()
            } else {
                saldo.multiply(i).redondearMonetario()
            }
            saldo = saldo.subtract(capitalCuota)
            CuotaCalculada(
                numero = indice + 1,
                fechaVencimiento = fechas[indice],
                valorTotal = capitalCuota.add(interesCuota).redondearMonetario(),
                capital = capitalCuota,
                interes = interesCuota
            )
        }
    }

    /** Cuotas con interÃ©s fijo sobre el capital original (sistema simple). */
    private fun cuotasInteresFijoSobreCapital(
        prestamo: Prestamo,
        capital: BigDecimal,
        n: Int
    ): List<CuotaCalculada> {
        val interesPorCuota = capital.multiply(prestamo.tasaValor)
            .divide(BigDecimal(100), contexto).redondearMonetario()
        val capitales = repartirCapital(capital, n)
        return capitales.mapIndexed { indice, capitalCuota ->
            CuotaCalculada(
                numero = indice + 1,
                fechaVencimiento = fechaDeCuota(prestamo, indice + 1),
                valorTotal = capitalCuota.add(interesPorCuota).redondearMonetario(),
                capital = capitalCuota,
                interes = interesPorCuota
            )
        }
    }

    /** Cuotas con interÃ©s plano: interÃ©s total repartido en partes iguales. */
    private fun cuotasInteresPlano(prestamo: Prestamo, capital: BigDecimal, n: Int): List<CuotaCalculada> {
        val interesTotal = if (prestamo.tasaTipo == TipoInteres.SIN_INTERES) {
            BigDecimal.ZERO
        } else {
            capital.multiply(prestamo.tasaValor).divide(BigDecimal(100), contexto)
                .redondearMonetario()
        }
        return combinarCapitalesIntereses(
            repartirCapital(capital, n),
            repartirCapital(interesTotal, n),
            (1..n).map { fechaDeCuota(prestamo, it) }
        )
    }

    /** Cuotas por saldo pendiente con cuota constante (francÃ©s). */
    private fun construirCuotasPorSaldo(
        prestamo: Prestamo,
        capital: BigDecimal,
        n: Int,
        tasa: BigDecimal,
        cuotaConstante: BigDecimal
    ): List<CuotaCalculada> {
        var saldo = capital
        val cuotas = ArrayList<CuotaCalculada>(n)
        for (numero in 1..n) {
            val esUltima = numero == n
            val interesCuota = saldo.multiply(tasa).redondearMonetario()
            val capitalCuota = if (esUltima) saldo else cuotaConstante.subtract(interesCuota)
            val totalCuota = capitalCuota.add(interesCuota).redondearMonetario()
            cuotas.add(
                CuotaCalculada(
                    numero = numero,
                    fechaVencimiento = fechaDeCuota(prestamo, numero),
                    valorTotal = totalCuota,
                    capital = capitalCuota,
                    interes = interesCuota
                )
            )
            saldo = saldo.subtract(capitalCuota)
        }
        return cuotas
    }

    // ------------------------------------------------------------------
    // Utilidades de cronograma
    // ------------------------------------------------------------------

    /**
     * Divide [total] en [partes] iguales; la Ãºltima parte absorbe la
     * diferencia de redondeo para que la suma sea exacta.
     */
    fun repartirCapital(total: BigDecimal, partes: Int): List<BigDecimal> {
        require(partes > 0) { "El nÃºmero de partes debe ser positivo." }
        if (partes == 1) return listOf(total.redondearMonetario())
        val porParte = total.divide(BigDecimal(partes), contexto).redondearMonetario()
        val resultado = ArrayList<BigDecimal>(partes)
        var resto = total
        for (i in 1..partes) {
            val valor = if (i == partes) resto.redondearMonetario() else porParte
            resultado.add(valor)
            resto = resto.subtract(valor)
        }
        return resultado
    }

    private fun combinarCapitalesIntereses(
        capitales: List<BigDecimal>,
        intereses: List<BigDecimal>,
        fechas: List<Long>
    ): List<CuotaCalculada> = capitales.mapIndexed { indice, capitalCuota ->
        val interesCuota = intereses[indice]
        CuotaCalculada(
            numero = indice + 1,
            fechaVencimiento = fechas[indice],
            valorTotal = capitalCuota.add(interesCuota).redondearMonetario(),
            capital = capitalCuota,
            interes = interesCuota
        )
    }

    /** Fecha de vencimiento de una cuota segÃºn la frecuencia del prÃ©stamo. */
    fun fechaDeCuota(prestamo: Prestamo, numero: Int): Long {
        if (numero <= 0) return prestamo.fechaDesembolso
        val inicio = Utilidades.fechaDeMilis(prestamo.fechaDesembolso)
        val fecha = sumarPeriodos(inicio, prestamo.frecuenciaCuotas, numero)
        val ultimoDiaMes = fecha.lengthOfMonth()
        val diaFinal = prestamo.diaVencimiento?.coerceIn(1, ultimoDiaMes) ?: fecha.dayOfMonth
        return Utilidades.milisDeFecha(fecha.withDayOfMonth(diaFinal))
    }

    /**
     * Calcula los intereses de mora de una cuota vencida.
     * Solo se aplican si el prÃ©stamo tiene la mora habilitada explÃ­citamente.
     */
    fun calcularMora(prestamo: Prestamo, cuota: Cuota, hoy: Long = System.currentTimeMillis()): BigDecimal {
        if (!prestamo.moraHabilitada || prestamo.moraTasa.signum() <= 0) return BigDecimal.ZERO
        if (cuota.estado == EstadoCuota.PAGADA) return BigDecimal.ZERO
        val diasRetraso = Utilidades.diasEntre(cuota.fechaVencimiento, hoy)
            .coerceAtLeast(0) - prestamo.periodoGraciaDias
        if (diasRetraso <= 0) return BigDecimal.ZERO
        val base = cuota.valorTotal.subtract(cuota.pagado).coerceAtLeast(BigDecimal.ZERO)
        return base.multiply(prestamo.moraTasa)
            .divide(BigDecimal(100), contexto)
            .multiply(BigDecimal(diasRetraso))
            .redondearMonetario()
    }

    /**
     * Aplica un pago sobre las cuotas pendientes en orden cronolÃ³gico.
     *
     * Prioridad de aplicaciÃ³n: mora â†’ intereses â†’ capital.
     * Devuelve las cuotas resultantes sin tocar la base de datos.
     */
    fun aplicarPago(
        cuotas: List<Cuota>,
        monto: BigDecimal,
        moraPendiente: BigDecimal = BigDecimal.ZERO,
        fechaPago: Long = System.currentTimeMillis()
    ): ParcialAplicacionPago {
        var restante = monto.redondearMonetario()
        var moraPagada = BigDecimal.ZERO
        var interesPagado = BigDecimal.ZERO
        var capitalPagado = BigDecimal.ZERO

        if (moraPendiente.signum() > 0 && restante.signum() > 0) {
            moraPagada = restante.min(moraPendiente).redondearMonetario()
            restante = restante.subtract(moraPagada)
        }

        val cuotasActualizadas = cuotas.sortedBy { it.numero }.map { cuota ->
            if (restante.signum() <= 0 || cuota.estado == EstadoCuota.PAGADA) {
                cuota
            } else {
                val pendienteCuota = cuota.valorTotal.subtract(cuota.pagado)
                    .coerceAtLeast(BigDecimal.ZERO)
                val interesPendiente = cuota.interes.subtract(
                    cuota.pagado.multiply(
                        if (cuota.valorTotal.signum() > 0)
                            cuota.interes.divide(cuota.valorTotal, contexto)
                        else BigDecimal.ZERO
                    )
                ).coerceAtLeast(BigDecimal.ZERO)
                val pagoEste = restante.min(pendienteCuota).redondearMonetario()
                if (pagoEste.signum() > 0) {
                    val pagoInteres = pagoEste.min(interesPendiente).redondearMonetario()
                    val pagoCapital = pagoEste.subtract(pagoInteres).redondearMonetario()
                    val nuevoPagado = cuota.pagado.add(pagoEste)
                    val nuevoEstado = when {
                        nuevoPagado >= cuota.valorTotal -> EstadoCuota.PAGADA
                        nuevoPagado.signum() > 0 -> EstadoCuota.PARCIAL
                        else -> cuota.estado
                    }
                    interesPagado = interesPagado.add(pagoInteres)
                    capitalPagado = capitalPagado.add(pagoCapital)
                    restante = restante.subtract(pagoEste)
                    cuota.copy(
                        pagado = nuevoPagado.redondearMonetario(),
                        estado = nuevoEstado,
                        fechaPago = if (nuevoEstado == EstadoCuota.PAGADA) fechaPago else cuota.fechaPago
                    )
                } else {
                    cuota
                }
            }
        }

        return ParcialAplicacionPago(
            cuotas = cuotasActualizadas,
            moraPagada = moraPagada.redondearMonetario(),
            interesPagado = interesPagado.redondearMonetario(),
            capitalPagado = capitalPagado.redondearMonetario(),
            montoSinAplicar = restante.redondearMonetario()
        )
    }

    /** Resultado de repartir un pago entre intereses y capital. */
    data class ParcialAplicacionPago(
        val cuotas: List<Cuota>,
        val moraPagada: BigDecimal,
        val interesPagado: BigDecimal,
        val capitalPagado: BigDecimal,
        /** Sobrante que no pudo aplicarse (pago mayor a la deuda). */
        val montoSinAplicar: BigDecimal
    )

    /** Estado financiero de un prÃ©stamo a partir de sus cuotas. */
    fun resumenPrestamo(prestamo: Prestamo, cuotas: List<Cuota>): ResumenPrestamo {
        val capitalPagado = cuotas.fold(BigDecimal.ZERO) { total, c ->
            total.add(
                c.pagado.multiply(
                    if (c.valorTotal.signum() > 0)
                        c.capital.divide(c.valorTotal, contexto)
                    else BigDecimal.ZERO
                )
            )
        }.redondearMonetario()
        val pagado = cuotas.fold(BigDecimal.ZERO) { total, c -> total.add(c.pagado) }
            .redondearMonetario()
        val interesesPrevistos = cuotas.fold(BigDecimal.ZERO) { total, c -> total.add(c.interes) }
            .redondearMonetario()
        val interesesPagados = pagado.subtract(capitalPagado).coerceAtLeast(BigDecimal.ZERO)
            .redondearMonetario()
        val capitalPendiente = prestamo.capital.subtract(capitalPagado).coerceAtLeast(BigDecimal.ZERO)
        val interesesPendientes = interesesPrevistos.subtract(interesesPagados)
            .coerceAtLeast(BigDecimal.ZERO)
        return ResumenPrestamo(
            capital = prestamo.capital.redondearMonetario(),
            capitalPagado = capitalPagado,
            capitalPendiente = capitalPendiente,
            interesesPrevistos = interesesPrevistos,
            interesesPagados = interesesPagados,
            interesesPendientes = interesesPendientes,
            totalPagado = pagado,
            totalPrestamo = prestamo.capital.add(interesesPrevistos).redondearMonetario(),
            cuotasPagadas = cuotas.count { it.estado == EstadoCuota.PAGADA },
            cuotasVencidas = cuotas.count {
                it.estado != EstadoCuota.PAGADA && it.fechaVencimiento < System.currentTimeMillis()
            },
            estaLiquidado = capitalPagado >= prestamo.capital
        )
    }
}

/** Resumen consolidado de la situaciÃ³n de un prÃ©stamo. */
data class ResumenPrestamo(
    val capital: BigDecimal,
    val capitalPagado: BigDecimal,
    val capitalPendiente: BigDecimal,
    val interesesPrevistos: BigDecimal,
    val interesesPagados: BigDecimal,
    val interesesPendientes: BigDecimal,
    val totalPagado: BigDecimal,
    val totalPrestamo: BigDecimal,
    val cuotasPagadas: Int,
    val cuotasVencidas: Int,
    val estaLiquidado: Boolean
)

