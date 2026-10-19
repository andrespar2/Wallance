package com.andrespar2.wallance.funcionalidades.ahorros

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Aporte
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.MetaAhorro
import com.andrespar2.wallance.nucleo.basedatos.repositorios.EstadoMeta
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioAhorros
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Estado de la pantalla de metas de ahorro. */
data class EstadoAhorros(
    val cargando: Boolean = true,
    val metas: List<EstadoMeta> = emptyList(),
    val cuentas: List<Cuenta> = emptyList(),
    val moneda: String = "COP"
)

@HiltViewModel
class AhorrosViewModel @Inject constructor(
    private val repositorio: RepositorioAhorros,
    private val repositorioCuentas: RepositorioCuentas,
    preferencias: RepositorioPreferencias
) : ViewModel() {

    val estado: StateFlow<EstadoAhorros> = combine(
        repositorio.metas(),
        repositorioCuentas.todasLasCuentas(),
        preferencias.monedaPrincipal,
        repositorio.todosLosAportes()
    ) { metas, cuentas, moneda, aportes ->
        val aportadoPorMeta = repositorio.sumarAportesPorMeta(aportes)
        EstadoAhorros(
            cargando = false,
            metas = metas.map { meta ->
                repositorio.estadoDe(meta, aportadoPorMeta[meta.id] ?: BigDecimal.ZERO)
            },
            cuentas = cuentas.map { it.cuenta },
            moneda = moneda
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoAhorros())

    var metaEnEdicion by mutableStateOf<MetaAhorro?>(null)
        private set

    var errorFormulario by mutableStateOf<String?>(null)
        private set

    var metaDetalle by mutableStateOf<MetaAhorro?>(null)
        private set

    var aportesDetalle by mutableStateOf<List<Aporte>>(emptyList())
        private set

    var errorAporte by mutableStateOf<String?>(null)
        private set

    private var trabajoAportes: Job? = null

    fun nuevaMeta(moneda: String) {
        errorFormulario = null
        metaEnEdicion = MetaAhorro(nombre = "", montoObjetivo = BigDecimal.ZERO, moneda = moneda)
    }

    fun editarMeta(meta: MetaAhorro) {
        errorFormulario = null
        metaEnEdicion = meta
    }

    fun cerrarFormulario() {
        metaEnEdicion = null
        errorFormulario = null
    }

    fun guardar(nombre: String, montoTexto: String, moneda: String, fechaObjetivo: Long?, cuentaId: Long?) {
        viewModelScope.launch {
            if (nombre.isBlank()) {
                errorFormulario = "El nombre de la meta es obligatorio."
                return@launch
            }
            val monto = Utilidades.textoADinero(montoTexto)
            if (monto == null || monto.signum() <= 0) {
                errorFormulario = "Ingresa un monto objetivo mayor que cero."
                return@launch
            }
            val base = metaEnEdicion ?: MetaAhorro(nombre = nombre, montoObjetivo = monto)
            try {
                repositorio.guardar(
                    base.copy(
                        nombre = nombre.trim(),
                        montoObjetivo = monto,
                        moneda = moneda,
                        fechaObjetivo = fechaObjetivo,
                        cuentaId = cuentaId,
                        activa = true
                    )
                )
                metaEnEdicion = null
                errorFormulario = null
            } catch (error: Exception) {
                errorFormulario = "No se pudo guardar la meta de ahorro."
            }
        }
    }

    fun eliminar(meta: MetaAhorro) {
        viewModelScope.launch {
            if (metaDetalle?.id == meta.id) cerrarDetalle()
            repositorio.eliminar(meta)
        }
    }

    fun abrirDetalle(meta: MetaAhorro) {
        metaDetalle = meta
        errorAporte = null
        trabajoAportes?.cancel()
        trabajoAportes = viewModelScope.launch {
            repositorio.aportesDe(meta.id).collect { aportesDetalle = it }
        }
    }

    fun cerrarDetalle() {
        trabajoAportes?.cancel()
        trabajoAportes = null
        metaDetalle = null
        aportesDetalle = emptyList()
        errorAporte = null
    }

    fun registrarAporte(montoTexto: String, cuentaId: Long?, fecha: Long) {
        val meta = metaDetalle ?: return
        viewModelScope.launch {
            val monto = Utilidades.textoADinero(montoTexto)
            if (monto == null || monto.signum() <= 0) {
                errorAporte = "El aporte debe ser mayor que cero."
                return@launch
            }
            try {
                repositorio.aportar(meta.id, monto, cuentaId, fecha, "")
                errorAporte = null
            } catch (error: Exception) {
                errorAporte = "No se pudo registrar el aporte."
            }
        }
    }
}
