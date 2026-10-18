package com.andrespar2.wallance.funcionalidades.inicio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.dao.MetaAhorroDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PrestamoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PresupuestoDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.repositorios.EstadoMeta
import com.andrespar2.wallance.nucleo.basedatos.repositorios.EstadoPresupuesto
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioContactos
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioAhorros
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioPresupuestos
import com.andrespar2.wallance.nucleo.comun.CalculoPeriodos
import com.andrespar2.wallance.nucleo.comun.IntervaloFechas
import com.andrespar2.wallance.nucleo.comun.RangoPeriodo
import com.andrespar2.wallance.nucleo.comun.Utilidades.redondearMonetario
import com.andrespar2.wallance.nucleo.basedatos.entidades.esGasto
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import com.andrespar2.wallance.nucleo.finanzas.MotorPrestamos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject

/** Próximo vencimiento mostrado en el panel. */
data class VencimientoCercano(
    val prestamo: Prestamo,
    val cuota: Cuota,
    val contacto: Contacto?
)

/** Resumen financiero completo del panel principal. */
data class ResumenPanel(
    val saldoTotal: BigDecimal = BigDecimal.ZERO,
    val ingresos: BigDecimal = BigDecimal.ZERO,
    val gastos: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val ahorroNeto: BigDecimal = BigDecimal.ZERO,
    val interesesCobrados: BigDecimal = BigDecimal.ZERO,
    val interesesPagados: BigDecimal = BigDecimal.ZERO,
    val dineroPrestado: BigDecimal = BigDecimal.ZERO,
    val dineroQueDebo: BigDecimal = BigDecimal.ZERO,
    val gananciaEsperada: BigDecimal = BigDecimal.ZERO,
    val gananciaRealizada: BigDecimal = BigDecimal.ZERO,
    val proximosVencimientos: List<VencimientoCercano> = emptyList(),
    val presupuestos: List<EstadoPresupuesto> = emptyList(),
    val metas: List<EstadoMeta> = emptyList(),
    val ultimosMovimientos: List<Movimiento> = emptyList()
)

/**
 * Controlador del panel principal.
 * Todos los datos provienen de la base de datos local; no hay datos ficticios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val cuentas: RepositorioCuentas,
    private val movimientoDao: MovimientoDao,
    private val prestamoDao: PrestamoDao,
    private val presupuestoDao: PresupuestoDao,
    private val metaAhorroDao: MetaAhorroDao,
    private val contactos: RepositorioContactos,
    private val preferencias: com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
) : ViewModel() {

    var periodoSeleccionado by mutableStateOf(RangoPeriodo.MES_ACTUAL)
        private set

    private val _periodo = MutableStateFlow(RangoPeriodo.MES_ACTUAL)

    val intervaloActual: IntervaloFechas
        get() = CalculoPeriodos.intervalo(periodoSeleccionado)

    fun cambiarPeriodo(periodo: RangoPeriodo) {
        periodoSeleccionado = periodo
        _periodo.value = periodo
    }

    val resumen: StateFlow<ResumenPanel?> = _periodo
        .flatMapLatest { periodo ->
            val intervalo = CalculoPeriodos.intervalo(periodo)
            combine(
                movimientoDao.entreFechas(intervalo.inicio, intervalo.fin),
                prestamoDao.prestamosActivos(),
                presupuestoDao.activos(),
                metaAhorroDao.todas()
            ) { movimientos, prestamos, _, metas ->
                Triple(movimientos, prestamos, metas)
            }
                .map { (movimientos, prestamos, metas) ->
                    construirResumen(intervalo, movimientos, prestamos, metas)
                }
                .flowOn(Dispatchers.Default)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun construirResumen(
        intervalo: IntervaloFechas,
        movimientos: List<Movimiento>,
        prestamos: List<Prestamo>,
        metas: List<com.andrespar2.wallance.nucleo.basedatos.entidades.MetaAhorro>
    ): ResumenPanel {
        val ingresos = movimientos.filter { it.tipo.esIngreso }
            .fold(BigDecimal.ZERO) { total, m -> total.add(m.montoEnPrincipal ?: m.monto) }
            .redondearMonetario()
        val gastos = movimientos.filter { it.tipo.esGasto }
            .fold(BigDecimal.ZERO) { total, m -> total.add(m.montoEnPrincipal ?: m.monto) }
            .redondearMonetario()
        val interesesCobrados = movimientos.filter {
            it.tipo == com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento.INTERESES_COBRADOS
        }.fold(BigDecimal.ZERO) { total, m -> total.add(m.montoEnPrincipal ?: m.monto) }.redondearMonetario()
        val interesesPagados = movimientos.filter {
            it.tipo == com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento.INTERESES_PAGADOS
        }.fold(BigDecimal.ZERO) { total, m -> total.add(m.montoEnPrincipal ?: m.monto) }.redondearMonetario()

        // Consolidado de préstamos activos.
        var dineroPrestado = BigDecimal.ZERO
        var dineroQueDebo = BigDecimal.ZERO
        var gananciaEsperada = BigDecimal.ZERO
        var gananciaRealizada = BigDecimal.ZERO
        val vencimientos = mutableListOf<VencimientoCercano>()

        for (prestamo in prestamos) {
            val cuotas = prestamoDao.cuotasDePrestamoSuspend(prestamo.id)
            val resumen = MotorPrestamos.resumenPrestamo(prestamo, cuotas)
            when (prestamo.tipo) {
                TipoPrestamo.OTORGADO -> {
                    dineroPrestado = dineroPrestado.add(resumen.capitalPendiente)
                    gananciaEsperada = gananciaEsperada.add(resumen.interesesPendientes)
                    gananciaRealizada = gananciaRealizada.add(resumen.interesesPagados)
                }
                TipoPrestamo.RECIBIDO -> {
                    dineroQueDebo = dineroQueDebo.add(resumen.capitalPendiente)
                    gananciaRealizada = gananciaRealizada
                        .subtract(resumen.interesesPagados).redondearMonetario()
                }
            }
            val proxima = cuotas
                .filter { it.estado != EstadoCuota.PAGADA }
                .minByOrNull { it.fechaVencimiento }
            if (proxima != null && prestamo.estado == EstadoPrestamo.ACTIVO) {
                vencimientos.add(
                    VencimientoCercano(prestamo, proxima, contactos.porId(prestamo.contactoId))
                )
            }
        }

        val presupuestos = estadoPresupuestos(intervalo)

        val estadosMetas = metas.filter { it.activa }.take(3).map { meta ->
            val aportado = metaAhorroDao.aportadoEnMetaSuspend(meta.id).redondearMonetario()
            val pendiente = meta.montoObjetivo.subtract(aportado).coerceAtLeast(BigDecimal.ZERO)
            val porcentaje = if (meta.montoObjetivo.signum() > 0) {
                aportado.multiply(BigDecimal(100))
                    .divide(meta.montoObjetivo, 2, java.math.RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            EstadoMeta(meta, aportado, pendiente, porcentaje)
        }

        return ResumenPanel(
            saldoTotal = cuentas.saldoTotal(),
            ingresos = ingresos,
            gastos = gastos,
            balance = ingresos.subtract(gastos).redondearMonetario(),
            ahorroNeto = ingresos.subtract(gastos).redondearMonetario(),
            interesesCobrados = interesesCobrados,
            interesesPagados = interesesPagados,
            dineroPrestado = dineroPrestado.redondearMonetario(),
            dineroQueDebo = dineroQueDebo.redondearMonetario(),
            gananciaEsperada = gananciaEsperada.redondearMonetario(),
            gananciaRealizada = gananciaRealizada.redondearMonetario(),
            proximosVencimientos = vencimientos.sortedBy { it.cuota.fechaVencimiento }.take(3),
            presupuestos = presupuestos,
            metas = estadosMetas,
            ultimosMovimientos = movimientos.sortedByDescending { it.fecha }.take(5)
        )
    }

    private suspend fun estadoPresupuestos(intervalo: IntervaloFechas): List<EstadoPresupuesto> {
        val resultado = mutableListOf<EstadoPresupuesto>()
        for (presupuesto in presupuestoDao.activosSuspend()) {
            val inicio = presupuesto.fechaInicio
            val fin = presupuesto.fechaFin ?: intervalo.fin
            val gastado = when {
                presupuesto.categoriaId != null ->
                    movimientoDao.gastoEnCategoria(presupuesto.categoriaId, inicio, fin)
                presupuesto.cuentaId != null ->
                    movimientoDao.gastoEnCuenta(presupuesto.cuentaId, inicio, fin)
                else -> BigDecimal.ZERO
            }.redondearMonetario()
            val porcentaje = if (presupuesto.monto.signum() > 0) {
                gastado.multiply(BigDecimal(100))
                    .divide(presupuesto.monto, 2, java.math.RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            resultado.add(
                EstadoPresupuesto(
                    presupuesto = presupuesto,
                    gastado = gastado,
                    disponible = presupuesto.monto.subtract(gastado).redondearMonetario(),
                    porcentajeConsumido = porcentaje
                )
            )
        }
        return resultado.sortedByDescending { it.porcentajeConsumido }.take(3)
    }

    val monedaPrincipal: Flow<String> = preferencias.monedaPrincipal
}
