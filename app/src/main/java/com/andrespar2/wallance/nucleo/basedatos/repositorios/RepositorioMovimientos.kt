package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.CategoriaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.esGasto
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/** Error controlado al registrar un movimiento. */
class ErrorRegistroMovimiento(mensaje: String) : Exception(mensaje)

/**
 * Repositorio de movimientos financieros.
 *
 * Responsable de validar y registrar ingresos, gastos, transferencias y
 * ajustes. Las transferencias entre cuentas propias nunca se contabilizan
 * como ingresos ni gastos.
 */
@Singleton
class RepositorioMovimientos @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao
) {

    fun todos(): Flow<List<Movimiento>> = movimientoDao.todos()

    fun entreFechas(inicio: Long, fin: Long): Flow<List<Movimiento>> =
        movimientoDao.entreFechas(inicio, fin)

    fun porPrestamo(prestamoId: Long): Flow<List<Movimiento>> =
        movimientoDao.porPrestamo(prestamoId)

    fun buscar(busqueda: String): Flow<List<Movimiento>> = movimientoDao.buscar(busqueda)

    fun cantidad(): Flow<Int> = movimientoDao.cantidad()

    suspend fun porId(id: Long): Movimiento? = movimientoDao.porId(id)

    /** Registra un movimiento validando su integridad. */
    suspend fun registrar(movimiento: Movimiento): Long {
        validar(movimiento)
        return movimientoDao.insertar(movimiento)
    }

    suspend fun editar(movimiento: Movimiento) {
        require(movimiento.id != 0L) { "El movimiento debe existir." }
        validar(movimiento)
        movimientoDao.actualizar(movimiento.copy(actualizadoEn = System.currentTimeMillis()))
    }

    suspend fun eliminar(id: Long) {
        movimientoDao.eliminarPorId(id)
    }

    fun ingresosEntre(inicio: Long, fin: Long): Flow<BigDecimal> =
        movimientoDao.ingresosEntre(inicio, fin)

    fun gastosEntre(inicio: Long, fin: Long): Flow<BigDecimal> =
        movimientoDao.gastosEntre(inicio, fin)

    fun interesesCobradosEntre(inicio: Long, fin: Long): Flow<BigDecimal> =
        movimientoDao.interesesCobradosEntre(inicio, fin)

    fun interesesPagadosEntre(inicio: Long, fin: Long): Flow<BigDecimal> =
        movimientoDao.interesesPagadosEntre(inicio, fin)

    suspend fun gastoEnCategoria(categoriaId: Long, inicio: Long, fin: Long): BigDecimal =
        movimientoDao.gastoEnCategoria(categoriaId, inicio, fin)

    private suspend fun validar(movimiento: Movimiento) {
        if (movimiento.monto.signum() <= 0) {
            throw ErrorRegistroMovimiento("El monto debe ser mayor que cero.")
        }
        if (movimiento.tipo == com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento.TRANSFERENCIA &&
            movimiento.cuentaDestinoId == null
        ) {
            throw ErrorRegistroMovimiento("Selecciona la cuenta de destino de la transferencia.")
        }
        if (movimiento.cuentaDestinoId != null &&
            movimiento.cuentaDestinoId == movimiento.cuentaId
        ) {
            throw ErrorRegistroMovimiento("La cuenta de origen y destino no pueden ser la misma.")
        }
        movimiento.categoriaId?.let { id ->
            val categoria = categoriaDao.porId(id)
                ?: throw ErrorRegistroMovimiento("La categoría seleccionada ya no existe.")
            val esperado = when {
                movimiento.tipo.esIngreso -> TipoCategoria.INGRESO
                movimiento.tipo.esGasto -> TipoCategoria.GASTO
                else -> null
            }
            if (esperado != null && categoria.tipo != esperado) {
                throw ErrorRegistroMovimiento("La categoría no corresponde al tipo de movimiento.")
            }
        }
    }
}
