package com.andrespar2.wallance.funcionalidades.suscripciones

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoSuscripcion
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.Suscripcion
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioSuscripciones
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.Utilidades.redondearMonetario
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/** Estado de la pantalla de suscripciones. */
data class EstadoSuscripciones(
    val cargando: Boolean = true,
    val suscripciones: List<Suscripcion> = emptyList(),
    val totalMensual: BigDecimal = BigDecimal.ZERO,
    val cuentas: List<Cuenta> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val moneda: String = "COP"
)

@HiltViewModel
class SuscripcionesViewModel @Inject constructor(
    private val repositorio: RepositorioSuscripciones,
    private val repositorioCuentas: RepositorioCuentas,
    private val repositorioCategorias: RepositorioCategorias,
    preferencias: RepositorioPreferencias
) : ViewModel() {

    val estado: StateFlow<EstadoSuscripciones> = combine(
        repositorio.todas(),
        repositorioCuentas.todasLasCuentas(),
        repositorioCategorias.todas(),
        preferencias.monedaPrincipal
    ) { suscripciones, cuentas, categorias, moneda ->
        EstadoSuscripciones(
            cargando = false,
            suscripciones = suscripciones,
            totalMensual = estimarMensual(suscripciones),
            cuentas = cuentas.map { it.cuenta },
            categorias = categorias.filter { !it.archivada },
            moneda = moneda
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoSuscripciones())

    var suscripcionEnEdicion by mutableStateOf<Suscripcion?>(null)
        private set

    var errorFormulario by mutableStateOf<String?>(null)
        private set

    fun nuevaSuscripcion() {
        errorFormulario = null
        suscripcionEnEdicion = Suscripcion(
            nombre = "",
            costo = BigDecimal.ZERO,
            proximoCobro = System.currentTimeMillis()
        )
    }

    fun editarSuscripcion(suscripcion: Suscripcion) {
        errorFormulario = null
        suscripcionEnEdicion = suscripcion
    }

    fun cerrarFormulario() {
        suscripcionEnEdicion = null
        errorFormulario = null
    }

    fun guardar(
        nombre: String,
        costoTexto: String,
        moneda: String,
        frecuencia: FrecuenciaCuota,
        proximoCobro: Long,
        cuentaId: Long?,
        categoriaId: Long?,
        estado: EstadoSuscripcion
    ) {
        viewModelScope.launch {
            if (nombre.isBlank()) {
                errorFormulario = "El nombre de la suscripción es obligatorio."
                return@launch
            }
            val costo = Utilidades.textoADinero(costoTexto)
            if (costo == null || costo.signum() <= 0) {
                errorFormulario = "Ingresa un costo mayor que cero."
                return@launch
            }
            val base = suscripcionEnEdicion
                ?: Suscripcion(nombre = nombre, costo = costo, proximoCobro = proximoCobro)
            try {
                repositorio.guardar(
                    base.copy(
                        nombre = nombre.trim(),
                        costo = costo,
                        moneda = moneda,
                        frecuencia = frecuencia,
                        proximoCobro = proximoCobro,
                        cuentaId = cuentaId,
                        categoriaId = categoriaId,
                        estado = estado
                    )
                )
                suscripcionEnEdicion = null
                errorFormulario = null
            } catch (error: Exception) {
                errorFormulario = "No se pudo guardar la suscripción."
            }
        }
    }

    fun cambiarEstado(suscripcion: Suscripcion, nuevo: EstadoSuscripcion) {
        viewModelScope.launch { repositorio.cambiarEstado(suscripcion, nuevo) }
    }

    fun eliminar(suscripcion: Suscripcion) {
        viewModelScope.launch { repositorio.eliminar(suscripcion) }
    }

    /**
     * Estima el costo mensual de las suscripciones activas normalizando cada
     * frecuencia a un mes. Las de pago único no se cuentan como recurrentes.
     */
    private fun estimarMensual(suscripciones: List<Suscripcion>): BigDecimal {
        val total = suscripciones.filter { it.estado == EstadoSuscripcion.ACTIVA }
            .fold(BigDecimal.ZERO) { acumulado, suscripcion ->
                acumulado.add(suscripcion.costo.multiply(factorMensual(suscripcion.frecuencia)))
            }
        return total.redondearMonetario()
    }

    private fun factorMensual(frecuencia: FrecuenciaCuota): BigDecimal = when (frecuencia) {
        FrecuenciaCuota.UNICA -> BigDecimal.ZERO
        FrecuenciaCuota.SEMANAL -> BigDecimal("4.3333")
        FrecuenciaCuota.QUINCENAL -> BigDecimal("2")
        FrecuenciaCuota.MENSUAL -> BigDecimal.ONE
        FrecuenciaCuota.BIMESTRAL -> BigDecimal.ONE.divide(BigDecimal("2"), 6, RoundingMode.HALF_UP)
        FrecuenciaCuota.TRIMESTRAL -> BigDecimal.ONE.divide(BigDecimal("3"), 6, RoundingMode.HALF_UP)
        FrecuenciaCuota.SEMESTRAL -> BigDecimal.ONE.divide(BigDecimal("6"), 6, RoundingMode.HALF_UP)
        FrecuenciaCuota.ANUAL -> BigDecimal.ONE.divide(BigDecimal("12"), 6, RoundingMode.HALF_UP)
    }
}
