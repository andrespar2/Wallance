package com.andrespar2.wallance.nucleo.red

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Cotizaciones devueltas por la fuente de divisas. */
data class RespuestaDivisas(
    /** Moneda base de la consulta. */
    val base: String,
    /** Unidades de cada moneda por una unidad de la base. */
    val tasas: Map<String, BigDecimal>,
    /** Instante de la última actualización de la fuente (segundos). */
    val fechaFuente: Long,
    /** Nombre de la fuente utilizada. */
    val fuente: String
)

/** Error controlado al consultar la fuente de divisas. */
class ErrorDivisas(mensaje: String, causa: Throwable? = null) : IOException(mensaje, causa)

/**
 * Cliente de la fuente pública de tipos de cambio (ExchangeRate-API).
 *
 * La API es gratuita, no requiere clave y publica una actualización diaria.
 * Wallance nunca inventa cotizaciones: si no hay conexión se usan los últimos
 * valores guardados y se informa su fecha.
 */
@Singleton
class ClienteDivisas @Inject constructor() {

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Consulta las tasas vigentes con base en el código indicado.
     * Lanza [ErrorDivisas] si no hay conexión o la respuesta no es válida.
     */
    suspend fun obtenerTasas(base: String): RespuestaDivisas = withContext(Dispatchers.IO) {
        val peticion = Request.Builder()
            .url("https://open.er-api.com/v6/latest/$base")
            .header("Accept", "application/json")
            .build()

        val respuesta = try {
            cliente.newCall(peticion).execute()
        } catch (error: IOException) {
            throw ErrorDivisas("No hay conexión con la fuente de divisas.", error)
        }

        respuesta.use {
            if (!it.isSuccessful) {
                throw ErrorDivisas("La fuente de divisas respondió con código ${it.code}.")
            }
            val cuerpo = it.body?.string()
                ?: throw ErrorDivisas("La fuente de divisas devolvió una respuesta vacía.")
            analizarRespuesta(cuerpo, base)
        }
    }

    private fun analizarRespuesta(cuerpo: String, baseSolicitada: String): RespuestaDivisas {
        val objeto = try {
            json.parseToJsonElement(cuerpo) as? JsonObject
        } catch (error: Throwable) {
            null
        } ?: throw ErrorDivisas("Respuesta no válida de la fuente de divisas.")

        val resultado = objeto["result"]?.jsonPrimitive?.contentOrNull
        if (resultado != "success") {
            throw ErrorDivisas("La fuente de divisas no devolvió tasas disponibles.")
        }

        val tasasObjeto = objeto["rates"] as? JsonObject
            ?: throw ErrorDivisas("La respuesta no incluye tasas de cambio.")

        val tasas = LinkedHashMap<String, BigDecimal>()
        for ((codigo, valor) in tasasObjeto) {
            val contenido = valor.jsonPrimitive.contentOrNull ?: continue
            val decimal = try {
                BigDecimal(contenido)
            } catch (error: NumberFormatException) {
                continue
            }
            if (decimal.signum() > 0) tasas[codigo] = decimal
        }
        if (tasas.isEmpty()) {
            throw ErrorDivisas("La fuente de divisas devolvió un conjunto de tasas vacío.")
        }

        val fechaFuente = objeto["time_last_update_unix"]?.jsonPrimitive?.longOrNull ?: 0L
        val fuente = objeto["provider"]?.jsonPrimitive?.contentOrNull ?: "ExchangeRate-API"
        return RespuestaDivisas(
            base = objeto["base_code"]?.jsonPrimitive?.contentOrNull ?: baseSolicitada,
            tasas = tasas,
            fechaFuente = fechaFuente,
            fuente = fuente
        )
    }
}
