package com.andrespar2.wallance.funcionalidades.cuentas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCuenta
import com.andrespar2.wallance.nucleo.basedatos.repositorios.CuentaConSaldo
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.comun.Utilidades
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Estado de la pantalla de cuentas.
 */
data class EstadoCuentas(
    val cargando: Boolean = true,
    val cuentas: List<CuentaConSaldo> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CuentasViewModel @Inject constructor(
    private val repositorio: RepositorioCuentas
) : ViewModel() {

    val estado = kotlinx.coroutines.flow.combine(
        repositorio.todasLasCuentas()
    ) { lista ->
        EstadoCuentas(cargando = false, cuentas = lista[0])
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCuentas())

    /** Cuenta en edición o null si no hay formulario abierto. */
    var cuentaEnEdicion by mutableStateOf<Cuenta?>(null)
        private set

    var errorFormulario by mutableStateOf<String?>(null)
        private set

    fun nuevaCuenta() {
        errorFormulario = null
        cuentaEnEdicion = Cuenta(nombre = "", moneda = "COP")
    }

    fun editarCuenta(cuenta: Cuenta) {
        errorFormulario = null
        cuentaEnEdicion = cuenta
    }

    fun cerrarFormulario() {
        cuentaEnEdicion = null
        errorFormulario = null
    }

    fun guardarCuenta(
        nombre: String,
        tipo: TipoCuenta,
        moneda: String,
        saldoInicialTexto: String
    ) {
        viewModelScope.launch {
            if (nombre.isBlank()) {
                errorFormulario = "El nombre de la cuenta es obligatorio."
                return@launch
            }
            val saldoInicial = Utilidades.textoADinero(saldoInicialTexto) ?: BigDecimal.ZERO
            val base = cuentaEnEdicion ?: Cuenta(nombre = nombre)
            try {
                repositorio.guardar(
                    base.copy(
                        nombre = nombre.trim(),
                        tipo = tipo,
                        moneda = moneda,
                        saldoInicial = saldoInicial
                    )
                )
                cuentaEnEdicion = null
                errorFormulario = null
            } catch (error: Exception) {
                errorFormulario = "No se pudo guardar la cuenta. ¿Existe otra con ese nombre?"
            }
        }
    }

    fun archivarCuenta(cuenta: Cuenta) {
        viewModelScope.launch { repositorio.eliminar(cuenta) }
    }

    fun restaurarCuenta(cuenta: Cuenta) {
        viewModelScope.launch { repositorio.restaurar(cuenta) }
    }
}
