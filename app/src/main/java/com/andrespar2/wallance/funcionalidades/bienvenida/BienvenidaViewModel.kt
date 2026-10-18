package com.andrespar2.wallance.funcionalidades.bienvenida

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCuenta
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import com.andrespar2.wallance.nucleo.seguridad.GestorSeguridad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Controlador del asistente de bienvenida de Wallance.
 *
 * La configuración inicial nunca exige crear cuenta en un servidor: todo se
 * guarda localmente y funciona sin conexión.
 */
@HiltViewModel
class BienvenidaViewModel @Inject constructor(
    private val cuentas: RepositorioCuentas,
    private val categorias: RepositorioCategorias,
    private val preferencias: RepositorioPreferencias
) : ViewModel() {

    var paso by mutableStateOf(0)
        private set

    var monedaSeleccionada by mutableStateOf("COP")
    var nombreCuenta by mutableStateOf("Efectivo")
    var tipoCuenta by mutableStateOf(TipoCuenta.EFECTIVO)
    var saldoInicialTexto by mutableStateOf("0")
    var pin by mutableStateOf("")
    var biometriaActiva by mutableStateOf(false)
    var notificacionesActivas by mutableStateOf(true)
    var consentimientoPublicidad by mutableStateOf(false)

    var cargando by mutableStateOf(false)
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set

    /** Número total de pasos del asistente. */
    val totalPasos = 7

    fun siguiente() {
        if (paso < totalPasos - 1) paso++
    }

    fun anterior() {
        if (paso > 0) paso--
    }

    fun validarPasoActual(): String? = when (paso) {
        2 -> if (monedaSeleccionada.isBlank()) "Selecciona una moneda principal." else null
        3 -> when {
            nombreCuenta.isBlank() -> "Escribe el nombre de tu primera cuenta."
            else -> null
        }
        4 -> if (pin.isNotBlank() && pin.length < 4) "El PIN debe tener al menos 4 dígitos." else null
        else -> null
    }

    /** Completa la configuración inicial y prepara el catálogo de categorías. */
    fun finalizar(onTerminado: () -> Unit) {
        cargando = true
        mensajeError = null
        viewModelScope.launch {
            try {
                preferencias.fijarMonedaPrincipal(monedaSeleccionada)
                categorias.siembraInicial()

                val saldoInicial = Utilidades.textoADinero(saldoInicialTexto) ?: BigDecimal.ZERO
                if (nombreCuenta.isNotBlank()) {
                    cuentas.guardar(
                        Cuenta(
                            nombre = nombreCuenta.trim(),
                            tipo = tipoCuenta,
                            moneda = monedaSeleccionada,
                            saldoInicial = saldoInicial
                        )
                    )
                }

                if (pin.isNotBlank()) {
                    preferencias.fijarPin(GestorSeguridad.hashPin(pin))
                    preferencias.fijarBiometria(biometriaActiva)
                }
                preferencias.fijarNotificaciones(notificacionesActivas)
                preferencias.fijarConsentimientoPublicidad(consentimientoPublicidad)
                preferencias.marcarConfiguracionCompletada()
                cargando = false
                onTerminado()
            } catch (error: Exception) {
                cargando = false
                mensajeError = "No fue posible completar la configuración: ${error.message}"
            }
        }
    }
}
