package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.CuentaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/** Cuenta junto con su saldo actual calculado. */
data class CuentaConSaldo(
    val cuenta: Cuenta,
    val saldo: BigDecimal
)

/**
 * Repositorio de cuentas.
 *
 * El saldo actual se reconstruye siempre a partir de los movimientos:
 * saldo inicial + entradas − salidas. Así no puede haber deriva entre el
 * saldo almacenado y el historial.
 */
@Singleton
class RepositorioCuentas @Inject constructor(
    private val cuentaDao: CuentaDao,
    private val movimientoDao: MovimientoDao
) {

    fun cuentasActivas(): Flow<List<CuentaConSaldo>> =
        cuentaDao.activas().map { lista -> lista.map { aConSaldo(it) } }

    fun todasLasCuentas(): Flow<List<CuentaConSaldo>> =
        cuentaDao.todas().map { lista -> lista.map { aConSaldo(it) } }

    fun cuenta(id: Long): Flow<Cuenta?> = cuentaDao.porIdFlujo(id)

    suspend fun cuentaPorId(id: Long): Cuenta? = cuentaDao.porId(id)

    suspend fun todasSuspend(): List<Cuenta> = cuentaDao.todasSuspend()

    suspend fun saldoDe(cuentaId: Long): BigDecimal {
        val cuenta = cuentaDao.porId(cuentaId) ?: return BigDecimal.ZERO
        return saldoCalculado(cuenta)
    }

    suspend fun saldoTotal(): BigDecimal =
        cuentaDao.todasSuspend().filter { it.activa }.fold(BigDecimal.ZERO) { total, cuenta ->
            total.add(saldoCalculado(cuenta))
        }

    suspend fun guardar(cuenta: Cuenta) {
        val existente = if (cuenta.id == 0L) null else cuentaDao.porId(cuenta.id)
        if (existente == null) {
            cuentaDao.insertar(cuenta)
        } else {
            cuentaDao.actualizar(cuenta)
        }
    }

    suspend fun eliminar(cuenta: Cuenta) {
        cuentaDao.actualizar(cuenta.copy(activa = false))
    }

    suspend fun restaurar(cuenta: Cuenta) {
        cuentaDao.actualizar(cuenta.copy(activa = true))
    }

    suspend fun contar(): Int = cuentaDao.todasSuspend().size

    private suspend fun aConSaldo(cuenta: Cuenta) =
        CuentaConSaldo(cuenta, saldoCalculado(cuenta))

    private suspend fun saldoCalculado(cuenta: Cuenta): BigDecimal =
        cuenta.saldoInicial.add(movimientoDao.balanceMovimientosCuenta(cuenta.id))
}
