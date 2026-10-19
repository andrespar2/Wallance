package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.CategoriaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.CuentaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PrestamoDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.Pago
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.SistemaAmortizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPago
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.comun.Utilidades.redondearMonetario
import com.andrespar2.wallance.nucleo.finanzas.MotorPrestamos
import com.andrespar2.wallance.nucleo.finanzas.ResumenPrestamo
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/** Error controlado al operar sobre un préstamo. */
class ErrorPrestamo(mensaje: String) : Exception(mensaje)

/**
 * Repositorio de préstamos personales.
 *
 * Reglas de contabilidad de Wallance:
 * - Prestar dinero disminuye el saldo disponible y aumenta la cuenta por
 *   cobrar; no es un gasto de consumo.
 * - Recuperar capital aumenta el saldo y disminuye la cuenta por cobrar; no
 *   es una ganancia.
 * - Cobrar intereses sí registra un ingreso financiero.
 * - Cada operación genera exactamente un registro de [Pago] y los movimientos
 *   asociados, sin duplicar información entre módulos.
 */
@Singleton
class RepositorioPrestamos @Inject constructor(
    private val prestamoDao: PrestamoDao,
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao,
    private val cuentaDao: CuentaDao
) {

    fun todos(): Flow<List<Prestamo>> = prestamoDao.todosLosPrestamos()

    suspend fun todosSuspend(): List<Prestamo> = prestamoDao.todosLosPrestamosSuspend()

    suspend fun cuotasDeSuspend(prestamoId: Long): List<Cuota> =
        prestamoDao.cuotasDePrestamoSuspend(prestamoId)

    suspend fun pagosDeSuspend(prestamoId: Long): List<Pago> =
        prestamoDao.pagosDePrestamoSuspend(prestamoId)

    fun activos(): Flow<List<Prestamo>> = prestamoDao.prestamosActivos()

    fun prestamo(id: Long): Flow<Prestamo?> = prestamoDao.prestamoPorIdFlujo(id)

    fun prestamosDeContacto(contactoId: Long): Flow<List<Prestamo>> =
        prestamoDao.prestamosDeContacto(contactoId)

    fun cuotasDe(prestamoId: Long) = prestamoDao.cuotasDePrestamo(prestamoId)

    fun pagosDe(prestamoId: Long) = prestamoDao.pagosDePrestamo(prestamoId)

    suspend fun prestamoPorId(id: Long): Prestamo? = prestamoDao.prestamoPorId(id)

    /**
     * Crea un préstamo y su cronograma de cuotas.
     * Las fechas personalizadas solo se usan con el sistema PERSONALIZADO.
     */
    suspend fun crearPrestamo(
        prestamo: Prestamo,
        fechasPersonalizadas: List<Long> = emptyList()
    ): Long {
        if (prestamo.capital.signum() <= 0) throw ErrorPrestamo("El capital debe ser mayor que cero.")
        if (prestamo.numeroCuotas < 1) throw ErrorPrestamo("Debe existir al menos una cuota.")
        if (prestamo.sistemaAmortizacion == SistemaAmortizacion.PERSONALIZADO &&
            fechasPersonalizadas.isEmpty()
        ) {
            throw ErrorPrestamo("Selecciona las fechas de vencimiento de las cuotas.")
        }

        val id = prestamoDao.insertarPrestamo(prestamo)
        val conId = prestamo.copy(id = id)
        val resumen = MotorPrestamos.calcularCronograma(conId, fechasPersonalizadas)
        prestamoDao.insertarCuotas(
            resumen.cuotas.map {
                com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota(
                    prestamoId = id,
                    numero = it.numero,
                    fechaVencimiento = it.fechaVencimiento,
                    valorTotal = it.valorTotal,
                    capital = it.capital,
                    interes = it.interes
                )
            }
        )
        return id
    }

    suspend fun actualizarPrestamo(prestamo: Prestamo) {
        prestamoDao.actualizarPrestamo(prestamo.copy(actualizadoEn = System.currentTimeMillis()))
    }

    /**
     * Registra un pago sobre un préstamo.
     *
     * Reparte el monto en mora, intereses y capital; actualiza las cuotas,
     * marca el préstamo como pagado cuando el capital queda liquidado y
     * genera los movimientos financieros asociados.
     */
    suspend fun registrarPago(
        prestamoId: Long,
        monto: BigDecimal,
        tipo: TipoPago,
        cuentaId: Long?,
        fecha: Long = System.currentTimeMillis(),
        nota: String = "",
        referencia: String = "",
        prestamo: Prestamo? = null
    ): Long {
        if (monto.signum() <= 0) throw ErrorPrestamo("El monto del pago debe ser mayor que cero.")
        val prestamoReal = prestamo ?: prestamoDao.prestamoPorId(prestamoId)
            ?: throw ErrorPrestamo("El préstamo ya no existe.")
        if (prestamoReal.estado == EstadoPrestamo.CANCELADO) {
            throw ErrorPrestamo("No se pueden registrar pagos sobre un préstamo cancelado.")
        }

        val cuotas = prestamoDao.cuotasDePrestamoSuspend(prestamoId)
        val moraPendiente = cuotas.filter { it.estado != EstadoCuota.PAGADA }
            .fold(BigDecimal.ZERO) { total, cuota ->
                total.add(MotorPrestamos.calcularMora(prestamoReal, cuota, fecha))
            }

        val aplicacion = MotorPrestamos.aplicarPago(cuotas, monto, moraPendiente, fecha)
        if (aplicacion.montoSinAplicar.signum() > 0) {
            throw ErrorPrestamo(
                "El pago supera la deuda pendiente. " +
                    "Monto disponible por aplicar: ${aplicacion.montoSinAplicar}."
            )
        }

        val montoAplicado = monto.subtract(aplicacion.montoSinAplicar).redondearMonetario()
        val pago = Pago(
            prestamoId = prestamoId,
            cuotaId = cuotas.firstOrNull { it.estado != EstadoCuota.PAGADA }?.id,
            fecha = fecha,
            monto = montoAplicado,
            capital = aplicacion.capitalPagado,
            interes = aplicacion.interesPagado,
            mora = aplicacion.moraPagada,
            tipo = tipo,
            cuentaId = cuentaId,
            nota = nota,
            referencia = referencia
        )
        val pagoId = prestamoDao.insertarPago(pago)

        aplicacion.cuotas.forEach { prestamoDao.actualizarCuota(it) }

        val resumen = MotorPrestamos.resumenPrestamo(prestamoReal, aplicacion.cuotas)
        val nuevoEstado = when {
            resumen.estaLiquidado -> EstadoPrestamo.PAGADO
            prestamoReal.fechaVencimiento < fecha && resumen.capitalPendiente.signum() > 0 ->
                EstadoPrestamo.VENCIDO
            else -> prestamoReal.estado
        }
        prestamoDao.actualizarPrestamo(prestamoReal.copy(estado = nuevoEstado))

        generarMovimientos(prestamoReal, pago, montoAplicado, fecha)
        return pagoId
    }

    /**
     * Genera los movimientos financieros de un pago.
     *
     * El capital recuperado no es una ganancia: se registra como cobro de
     * préstamo. Los intereses y la mora sí son ingresos (o gastos, según la
     * dirección del préstamo).
     */
    private suspend fun generarMovimientos(prestamo: Prestamo, pago: Pago, monto: BigDecimal, fecha: Long) {
        val categoriaIngreso = categoriaIngresoIntereses()
        val categoriaGasto = categoriaGastoIntereses()

        suspend fun insertar(tipo: TipoMovimiento, montoMov: BigDecimal, categoriaId: Long?) {            if (montoMov.signum() <= 0) return
            val cuentaIdBase = pago.cuentaId ?: return
            movimientoDao.insertar(
                Movimiento(
                    tipo = tipo,
                    monto = montoMov,
                    moneda = prestamo.moneda,
                    categoriaId = categoriaId,
                    cuentaId = cuentaIdBase,
                    fecha = fecha,
                    descripcion = when (tipo) {
                        TipoMovimiento.INTERESES_COBRADOS -> "Intereses de préstamo"
                        TipoMovimiento.INTERESES_PAGADOS -> "Intereses de préstamo"
                        else -> "Pago de préstamo"
                    },
                    prestamoId = prestamo.id,
                    notas = pago.nota,
                    creadoEn = fecha,
                    actualizadoEn = fecha
                )
            )
        }

        val capitalMasMora = pago.capital.add(pago.mora)
        val interes = pago.interes
        when (prestamo.tipo) {
            TipoPrestamo.OTORGADO -> {
                insertar(TipoMovimiento.COBRO_PRESTAMO, capitalMasMora, null)
                insertar(TipoMovimiento.INTERESES_COBRADOS, interes, categoriaIngreso)
            }
            TipoPrestamo.RECIBIDO -> {
                insertar(TipoMovimiento.PAGO_DEUDA, capitalMasMora, null)
                insertar(TipoMovimiento.INTERESES_PAGADOS, interes, categoriaGasto)
            }
        }
    }

    /** Registra un nuevo desembolso de capital sobre un préstamo existente. */
    suspend fun registrarDesembolso(
        prestamoId: Long,
        monto: BigDecimal,
        cuentaId: Long?,
        fecha: Long = System.currentTimeMillis(),
        nota: String = ""
    ): Long {
        val prestamo = prestamoDao.prestamoPorId(prestamoId)
            ?: throw ErrorPrestamo("El préstamo ya no existe.")
        val pago = Pago(
            prestamoId = prestamoId,
            fecha = fecha,
            monto = monto,
            capital = monto,
            interes = BigDecimal.ZERO,
            tipo = TipoPago.NUEVO_DESEMBOLSO,
            cuentaId = cuentaId,
            nota = nota
        )
        val id = prestamoDao.insertarPago(pago)
        if (cuentaId != null) {
            movimientoDao.insertar(
                Movimiento(
                    tipo = when (prestamo.tipo) {
                        TipoPrestamo.OTORGADO -> TipoMovimiento.PRESTAMO_OTORGADO
                        TipoPrestamo.RECIBIDO -> TipoMovimiento.PRESTAMO_RECIBIDO
                    },
                    monto = monto,
                    moneda = prestamo.moneda,
                    cuentaId = cuentaId,
                    fecha = fecha,
                    descripcion = "Desembolso de préstamo",
                    prestamoId = prestamoId,
                    notas = nota,
                    creadoEn = fecha,
                    actualizadoEn = fecha
                )
            )
        }
        return id
    }

    /** Cancela un préstamo sin borrar su historial de operaciones. */
    suspend fun cancelarPrestamo(prestamoId: Long) {
        val prestamo = prestamoDao.prestamoPorId(prestamoId)
            ?: throw ErrorPrestamo("El préstamo ya no existe.")
        prestamoDao.actualizarPrestamo(prestamo.copy(estado = EstadoPrestamo.CANCELADO))
    }

    /**
     * Elimina un préstamo con sus cuotas y pagos, además de los movimientos
     * financieros que originó, para no dejar cifras huérfanas.
     */
    suspend fun eliminarPrestamo(prestamoId: Long) {
        val prestamo = prestamoDao.prestamoPorId(prestamoId)
            ?: throw ErrorPrestamo("El préstamo ya no existe.")
        movimientoDao.porPrestamoSuspend(prestamoId).forEach { movimientoDao.eliminarPorId(it.id) }
        prestamoDao.eliminarPrestamoCompleto(prestamo)
    }

    suspend fun resumen(prestamo: Prestamo): ResumenPrestamo {
        val cuotas = prestamoDao.cuotasDePrestamoSuspend(prestamo.id)
        return MotorPrestamos.resumenPrestamo(prestamo, cuotas)
    }

    /** Consolidado financiero de un contacto (todas sus obligaciones). */
    suspend fun resumenDeContacto(contactoId: Long): ResumenContacto {
        val prestamos = prestamoDao.prestamosDeContactoSuspend(contactoId)
        var capitalPrestado = BigDecimal.ZERO
        var capitalPagado = BigDecimal.ZERO
        var capitalPendiente = BigDecimal.ZERO
        var interesesPrevistos = BigDecimal.ZERO
        var interesesCobrados = BigDecimal.ZERO
        var interesesPagadosTotal = BigDecimal.ZERO
        var gananciaEsperada = BigDecimal.ZERO
        var gananciaRealizada = BigDecimal.ZERO

        for (prestamo in prestamos) {
            val cuotas = prestamoDao.cuotasDePrestamoSuspend(prestamo.id)
            val resumen = MotorPrestamos.resumenPrestamo(prestamo, cuotas)
            capitalPrestado = capitalPrestado.add(resumen.capital)
            capitalPagado = capitalPagado.add(resumen.capitalPagado)
            capitalPendiente = capitalPendiente.add(resumen.capitalPendiente)
            interesesPrevistos = interesesPrevistos.add(resumen.interesesPrevistos)
            when (prestamo.tipo) {
                TipoPrestamo.OTORGADO -> {
                    interesesCobrados = interesesCobrados.add(resumen.interesesPagados)
                    gananciaEsperada = gananciaEsperada.add(resumen.interesesPendientes)
                    gananciaRealizada = gananciaRealizada.add(resumen.interesesPagados)
                }
                TipoPrestamo.RECIBIDO -> {
                    interesesPagadosTotal = interesesPagadosTotal.add(resumen.interesesPagados)
                }
            }
        }

        return ResumenContacto(
            totalPrestamos = prestamos.size,
            prestamosActivos = prestamos.count {
                it.estado == EstadoPrestamo.ACTIVO || it.estado == EstadoPrestamo.VENCIDO
            },
            prestamosPagados = prestamos.count { it.estado == EstadoPrestamo.PAGADO },
            prestamosVencidos = prestamos.count { it.estado == EstadoPrestamo.VENCIDO },
            capitalPrestado = capitalPrestado.redondearMonetario(),
            capitalPagado = capitalPagado.redondearMonetario(),
            capitalPendiente = capitalPendiente.redondearMonetario(),
            interesesPrevistos = interesesPrevistos.redondearMonetario(),
            interesesCobrados = interesesCobrados.redondearMonetario(),
            interesesPagados = interesesPagadosTotal.redondearMonetario(),
            gananciaEsperada = gananciaEsperada.redondearMonetario(),
            gananciaRealizada = gananciaRealizada.redondearMonetario()
        )
    }

    private suspend fun categoriaIngresoIntereses(): Long? =
        categoriaDao.activasPorTipoSuspend(com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria.INGRESO)
            .firstOrNull { it.nombre == "Intereses" }?.id

    private suspend fun categoriaGastoIntereses(): Long? =
        categoriaDao.activasPorTipoSuspend(com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria.GASTO)
            .firstOrNull { it.nombre == "Intereses" || it.nombre == "Comisiones" }?.id
}

/** Consolidado financiero de un contacto. */
data class ResumenContacto(
    val totalPrestamos: Int,
    val prestamosActivos: Int,
    val prestamosPagados: Int,
    val prestamosVencidos: Int,
    val capitalPrestado: BigDecimal,
    val capitalPagado: BigDecimal,
    val capitalPendiente: BigDecimal,
    val interesesPrevistos: BigDecimal,
    val interesesCobrados: BigDecimal,
    val interesesPagados: BigDecimal,
    val gananciaEsperada: BigDecimal,
    val gananciaRealizada: BigDecimal
)
