package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.MetaAhorroDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PresupuestoDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Aporte
import com.andrespar2.wallance.nucleo.basedatos.entidades.MetaAhorro
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.Presupuesto
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento
import com.andrespar2.wallance.nucleo.comun.Utilidades.redondearMonetario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de un presupuesto comparado con el gasto real. */
data class EstadoPresupuesto(
    val presupuesto: Presupuesto,
    val gastado: BigDecimal,
    val disponible: BigDecimal,
    val porcentajeConsumido: BigDecimal
)

/** Estado de una meta de ahorro. */
data class EstadoMeta(
    val meta: MetaAhorro,
    val aportado: BigDecimal,
    val pendiente: BigDecimal,
    val porcentaje: BigDecimal
)

@Singleton
class RepositorioPresupuestos @Inject constructor(
    private val presupuestoDao: PresupuestoDao,
    private val movimientoDao: MovimientoDao
) {

    fun todos(): Flow<List<Presupuesto>> = presupuestoDao.todos()

    fun activos(): Flow<List<Presupuesto>> = presupuestoDao.activos()

    suspend fun guardar(presupuesto: Presupuesto): Long =
        if (presupuesto.id == 0L) {
            presupuestoDao.insertar(presupuesto)
        } else {
            presupuestoDao.actualizar(presupuesto)
            presupuesto.id
        }

    suspend fun eliminar(presupuesto: Presupuesto) = presupuestoDao.eliminar(presupuesto)

    /** Combina cada presupuesto con el gasto acumulado en su periodo. */
    fun estados(inicioPeriodoGlobal: Long, finPeriodoGlobal: Long): Flow<List<EstadoPresupuesto>> =
        presupuestoDao.activos().map { lista ->
            lista.map { estadoDe(it, finPeriodoGlobal) }
        }

    private suspend fun estadoDe(presupuesto: Presupuesto, finGlobal: Long): EstadoPresupuesto {
        val inicio = presupuesto.fechaInicio
        val fin = presupuesto.fechaFin ?: finGlobal
        val gastado = when {
            presupuesto.categoriaId != null ->
                movimientoDao.gastoEnCategoria(presupuesto.categoriaId, inicio, fin)
            presupuesto.cuentaId != null ->
                movimientoDao.gastoEnCuenta(presupuesto.cuentaId, inicio, fin)
            else -> totalGastoEntre(inicio, fin)
        }.redondearMonetario()

        val disponible = presupuesto.monto.subtract(gastado).redondearMonetario()
        val porcentaje = if (presupuesto.monto.signum() > 0) {
            gastado.multiply(BigDecimal(100))
                .divide(presupuesto.monto, 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        return EstadoPresupuesto(presupuesto, gastado, disponible, porcentaje)
    }

    private suspend fun totalGastoEntre(inicio: Long, fin: Long): BigDecimal =
        movimientoDao.entreFechas(inicio, fin).first().fold(BigDecimal.ZERO) { total, movimiento ->
            if (movimiento.tipo == TipoMovimiento.GASTO || movimiento.tipo == TipoMovimiento.COMISION) {
                total.add(movimiento.montoEnPrincipal ?: movimiento.monto)
            } else {
                total
            }
        }
}

@Singleton
class RepositorioAhorros @Inject constructor(
    private val metaAhorroDao: MetaAhorroDao,
    private val movimientoDao: MovimientoDao
) {

    fun metas(): Flow<List<MetaAhorro>> = metaAhorroDao.todas()

    fun aportesDe(metaId: Long): Flow<List<Aporte>> = metaAhorroDao.aportesDeMeta(metaId)

    suspend fun metaPorId(id: Long): MetaAhorro? = metaAhorroDao.metaPorId(id)

    suspend fun guardar(meta: MetaAhorro): Long =
        if (meta.id == 0L) {
            metaAhorroDao.insertar(meta)
        } else {
            metaAhorroDao.actualizar(meta)
            meta.id
        }

    suspend fun eliminar(meta: MetaAhorro) = metaAhorroDao.eliminar(meta)

    /**
     * Registra un aporte a una meta.
     *
     * La meta es una etiqueta de seguimiento, no una cuenta adicional: si el
     * aporte proviene de una cuenta, se registra un ajuste de salida para que
     * el dinero no quede contabilizado dos veces.
     */
    suspend fun aportar(
        metaId: Long,
        monto: BigDecimal,
        cuentaId: Long?,
        fecha: Long,
        nota: String = ""
    ): Long {
        require(monto.signum() > 0) { "El aporte debe ser mayor que cero." }
        val movimientoId = if (cuentaId != null) {
            movimientoDao.insertar(
                Movimiento(
                    tipo = TipoMovimiento.AJUSTE,
                    monto = monto,
                    cuentaId = cuentaId,
                    fecha = fecha,
                    descripcion = "Aporte a meta de ahorro",
                    notas = nota,
                    ajusteEsAumento = false,
                    creadoEn = fecha,
                    actualizadoEn = fecha
                )
            )
        } else {
            null
        }
        return metaAhorroDao.insertarAporte(
            Aporte(
                metaId = metaId,
                monto = monto,
                fecha = fecha,
                cuentaId = cuentaId,
                movimientoId = movimientoId,
                nota = nota
            )
        )
    }

    fun aportadoEn(metaId: Long): Flow<BigDecimal> = metaAhorroDao.aportadoEnMeta(metaId)

    suspend fun estadoDe(meta: MetaAhorro): EstadoMeta {
        val aportado = metaAhorroDao.aportadoEnMetaSuspend(meta.id).redondearMonetario()
        val pendiente = meta.montoObjetivo.subtract(aportado).coerceAtLeast(BigDecimal.ZERO)
        val porcentaje = if (meta.montoObjetivo.signum() > 0) {
            aportado.multiply(BigDecimal(100))
                .divide(meta.montoObjetivo, 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        return EstadoMeta(meta, aportado, pendiente, porcentaje)
    }
}
