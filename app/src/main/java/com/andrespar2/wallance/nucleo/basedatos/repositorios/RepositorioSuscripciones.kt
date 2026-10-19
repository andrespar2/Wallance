package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.SuscripcionDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoSuscripcion
import com.andrespar2.wallance.nucleo.basedatos.entidades.Suscripcion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio de suscripciones y gastos recurrentes.
 *
 * Las suscripciones son un recordatorio de cobros periódicos: no generan
 * movimientos por sí solas. La contabilización ocurre cuando el usuario
 * registra el gasto real.
 */
@Singleton
class RepositorioSuscripciones @Inject constructor(
    private val suscripcionDao: SuscripcionDao
) {

    fun todas(): Flow<List<Suscripcion>> = suscripcionDao.todas()

    fun activas(): Flow<List<Suscripcion>> = suscripcionDao.activas()

    suspend fun porId(id: Long): Suscripcion? = suscripcionDao.porId(id)

    suspend fun guardar(suscripcion: Suscripcion): Long =
        if (suscripcion.id == 0L) {
            suscripcionDao.insertar(suscripcion)
        } else {
            suscripcionDao.actualizar(suscripcion)
            suscripcion.id
        }

    suspend fun cambiarEstado(suscripcion: Suscripcion, estado: EstadoSuscripcion) {
        suscripcionDao.actualizar(suscripcion.copy(estado = estado))
    }

    suspend fun eliminar(suscripcion: Suscripcion) = suscripcionDao.eliminar(suscripcion)

    /** Total mensual estimado de las suscripciones activas. */
    suspend fun totalMensualEstimado(): BigDecimal =
        suscripcionDao.activas().first().fold(BigDecimal.ZERO) { acumulado, suscripcion ->
            acumulado.add(suscripcion.costo)
        }
}
