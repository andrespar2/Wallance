package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.CotizacionDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cotizacion
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas.normalizarTasa
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import com.andrespar2.wallance.nucleo.red.ClienteDivisas
import com.andrespar2.wallance.nucleo.red.ErrorDivisas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de una actualización de cotizaciones. */
data class ResultadoActualizacion(
    val exitosa: Boolean,
    val mensaje: String,
    val fechaGuardado: Long = 0L,
    val fuente: String = ""
)

/**
 * Repositorio de divisas.
 *
 * Estrategia offline-first:
 * 1. Si la caché es reciente, se usa sin consultar la red.
 * 2. Si hay conexión, se actualiza y se guardan las tasas con su fecha y fuente.
 * 3. Sin conexión, se informa la última actualización disponible.
 * Nunca se inventan cotizaciones.
 */
@Singleton
class RepositorioDivisas @Inject constructor(
    private val clienteDivisas: ClienteDivisas,
    private val cotizacionDao: CotizacionDao,
    private val preferencias: RepositorioPreferencias
) {

    /** Vigencia de una cotización diaria: 12 horas. */
    private val vigenciaMillis = TimeUnit.HOURS.toMillis(12)

    fun cotizacionesGuardadas(): Flow<List<Cotizacion>> = cotizacionDao.todas()

    suspend fun monedaPrincipal(): String = preferencias.monedaPrincipal.first()

    /**
     * Devuelve la tasa para convertir de [origen] a [destino], consultando la
     * red solo si la caché no está vigente. Devuelve null si no hay datos.
     */
    suspend fun tasa(origen: String, destino: String, forzarActualizacion: Boolean = false): BigDecimal? {
        if (origen == destino) return BigDecimal.ONE
        val directa = cotizacionDao.obtener(origen, destino)
        val vigente = directa != null &&
            !forzarActualizacion &&
            System.currentTimeMillis() - directa.fechaConsulta < vigenciaMillis

        if (!vigente) {
            try {
                actualizar(origen)
            } catch (error: ErrorDivisas) {
                // Se conserva la caché existente: se informará su fecha.
            }
        }
        val guardada = cotizacionDao.obtener(origen, destino)
        if (guardada != null) return guardada.tasa

        // Caso cruzado: ambas monedas cotizadas contra la misma base.
        return tasaCruzada(origen, destino)
    }

    /** Convierte una cantidad entre monedas usando las tasas disponibles. */
    suspend fun convertir(cantidad: BigDecimal, origen: String, destino: String): BigDecimal? {
        if (origen == destino) return cantidad
        val tasa = tasa(origen, destino) ?: return null
        return cantidad.multiply(tasa).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Descarga las tasas con base en [base] y las guarda junto con sus
     * pares inversos derivados.
     */
    suspend fun actualizar(base: String): ResultadoActualizacion {
        val respuesta = try {
            clienteDivisas.obtenerTasas(base)
        } catch (error: ErrorDivisas) {
            return ResultadoActualizacion(
                exitosa = false,
                mensaje = "No fue posible actualizar las cotizaciones: ${error.message}"
            )
        }

        val ahora = System.currentTimeMillis()
        val fechaFuente = if (respuesta.fechaFuente > 0)
            TimeUnit.SECONDS.toMillis(respuesta.fechaFuente) else ahora

        val cotizaciones = respuesta.tasas.map { (destino, tasa) ->
            Cotizacion(
                base = base,
                destino = destino,
                tasa = normalizarTasa(tasa),
                fechaConsulta = fechaFuente,
                fuente = respuesta.fuente
            )
        }
        cotizaciones.forEach { cotizacionDao.guardar(it) }
        preferencias.fijarUltimaActualizacionDivisas(ahora)

        return ResultadoActualizacion(
            exitosa = true,
            mensaje = "Cotizaciones actualizadas.",
            fechaGuardado = fechaFuente,
            fuente = respuesta.fuente
        )
    }

    private suspend fun tasaCruzada(origen: String, destino: String): BigDecimal? {
        // Se busca una moneda intermedia con la que ambas tengan cotización.
        val cotizaciones = listOfNotNull(
            cotizacionDao.obtener("USD", origen),
            cotizacionDao.obtener("USD", destino)
        )
        if (cotizaciones.size == 2) {
            val tasaOrigen = cotizaciones[0].tasa
            val tasaDestino = cotizaciones[1].tasa
            return MotorDivisas.tasaCruzada(tasaDestino, tasaOrigen)
        }
        return null
    }
}
