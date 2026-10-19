package com.andrespar2.wallance.funcionalidades.divisas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cotizacion
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioDivisas
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/** Una pareja de cotización lista para mostrar. */
data class FilaDivisa(
    val base: String,
    val destino: String,
    val tasa: BigDecimal
)

/** Estado de la pantalla de divisas. */
data class EstadoDivisas(
    val cargando: Boolean = true,
    val monedaPrincipal: String = "COP",
    val parejas: List<FilaDivisa> = emptyList(),
    val fechaActualizacion: Long = 0L,
    val fuente: String = "",
    val actualizando: Boolean = false,
    val mensaje: String? = null
)

@HiltViewModel
class DivisasViewModel @Inject constructor(
    private val repositorio: RepositorioDivisas,
    private val preferencias: RepositorioPreferencias
) : ViewModel() {

    private val actualizando = MutableStateFlow(false)
    private val mensaje = MutableStateFlow<String?>(null)

    val estado: StateFlow<EstadoDivisas> = combine(
        repositorio.cotizacionesGuardadas(),
        preferencias.monedaPrincipal,
        actualizando,
        mensaje
    ) { cotizaciones, principal, enProceso, aviso ->
        val fecha = cotizaciones.maxOfOrNull { it.fechaConsulta } ?: 0L
        val fuente = cotizaciones.maxByOrNull { it.fechaConsulta }?.fuente.orEmpty()
        EstadoDivisas(
            cargando = false,
            monedaPrincipal = principal,
            parejas = construirParejas(cotizaciones, principal),
            fechaActualizacion = fecha,
            fuente = fuente,
            actualizando = enProceso,
            mensaje = aviso
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDivisas())

    /** Resultado de la última conversión solicitada. */
    var conversion by mutableStateOf<String?>(null)
        private set

    var conversionExitosa by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { actualizar(silencioso = true) }
    }

    fun actualizar(silencioso: Boolean = false) {
        viewModelScope.launch {
            actualizando.value = true
            if (!silencioso) mensaje.value = null
            val base = preferencias.monedaPrincipal.first()
            val resultado = repositorio.actualizar(base)
            actualizando.value = false
            mensaje.value = if (resultado.exitosa) {
                if (silencioso) null else "Cotizaciones actualizadas."
            } else {
                resultado.mensaje
            }
        }
    }

    fun convertir(texto: String, origen: String, destino: String) {
        viewModelScope.launch {
            val cantidad = Utilidades.textoADinero(texto)
            if (cantidad == null || cantidad.signum() <= 0) {
                conversion = "Ingresa una cantidad mayor que cero."
                conversionExitosa = false
                return@launch
            }
            val resultado = repositorio.convertir(cantidad, origen, destino)
            if (resultado == null) {
                conversion = "No hay cotización disponible para ese par. Actualiza las tasas."
                conversionExitosa = false
            } else {
                conversion = "${Utilidades.formatearDinero(cantidad, origen)} = " +
                    Utilidades.formatearDinero(resultado, destino)
                conversionExitosa = true
            }
        }
    }

    fun limpiarConversion() {
        conversion = null
        conversionExitosa = false
    }

    /** Deriva las parejas sugeridas a partir de las cotizaciones guardadas. */
    private fun construirParejas(
        cotizaciones: List<Cotizacion>,
        principal: String
    ): List<FilaDivisa> {
        val mapa = cotizaciones.associateBy { it.base to it.destino }

        fun tasa(base: String, destino: String): BigDecimal? {
            mapa[base to destino]?.let { return it.tasa }
            mapa[destino to base]?.let {
                if (it.tasa.signum() > 0) {
                    return BigDecimal.ONE.divide(it.tasa, 6, RoundingMode.HALF_UP)
                }
            }
            val haciaBase = mapa[principal to base]?.tasa
                ?: mapa[base to principal]?.tasa?.let {
                    if (it.signum() > 0) BigDecimal.ONE.divide(it, 6, RoundingMode.HALF_UP) else null
                }
            val haciaDestino = mapa[principal to destino]?.tasa
                ?: mapa[destino to principal]?.tasa?.let {
                    if (it.signum() > 0) BigDecimal.ONE.divide(it, 6, RoundingMode.HALF_UP) else null
                }
            return MotorDivisas.tasaCruzada(haciaDestino, haciaBase)
        }

        return MotorDivisas.parejasSugeridas(principal).mapNotNull { (base, destino) ->
            tasa(base, destino)?.let { FilaDivisa(base, destino, it) }
        }
    }
}
