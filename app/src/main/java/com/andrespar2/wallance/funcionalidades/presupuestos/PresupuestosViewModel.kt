package com.andrespar2.wallance.funcionalidades.presupuestos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.Presupuesto
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPeriodoPresupuesto
import com.andrespar2.wallance.nucleo.basedatos.repositorios.EstadoPresupuesto
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioPresupuestos
import com.andrespar2.wallance.nucleo.comun.CalculoPeriodos
import com.andrespar2.wallance.nucleo.comun.RangoPeriodo
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Estado de la pantalla de presupuestos. */
data class EstadoPresupuestos(
    val cargando: Boolean = true,
    val presupuestos: List<EstadoPresupuesto> = emptyList(),
    val categorias: Map<Long, Categoria> = emptyMap(),
    val cuentas: Map<Long, Cuenta> = emptyMap(),
    val moneda: String = "COP"
)

@HiltViewModel
class PresupuestosViewModel @Inject constructor(
    private val repositorio: RepositorioPresupuestos,
    private val repositorioCategorias: RepositorioCategorias,
    private val repositorioCuentas: RepositorioCuentas,
    preferencias: RepositorioPreferencias
) : ViewModel() {

    private val intervalo = CalculoPeriodos.intervalo(RangoPeriodo.MES_ACTUAL)

    val estado: StateFlow<EstadoPresupuestos> = combine(
        repositorio.estados(intervalo.inicio, intervalo.fin),
        repositorioCategorias.todas(),
        repositorioCuentas.todasLasCuentas(),
        preferencias.monedaPrincipal
    ) { presupuestos, categorias, cuentas, moneda ->
        EstadoPresupuestos(
            cargando = false,
            presupuestos = presupuestos,
            categorias = categorias.associateBy { it.id },
            cuentas = cuentas.associate { it.cuenta.id to it.cuenta },
            moneda = moneda
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPresupuestos())

    var presupuestoEnEdicion by androidx.compose.runtime.mutableStateOf<Presupuesto?>(null)
        private set

    var errorFormulario by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    fun nuevoPresupuesto() {
        errorFormulario = null
        val (inicio, fin) = rangoPredeterminado(TipoPeriodoPresupuesto.MENSUAL)
        presupuestoEnEdicion = Presupuesto(
            nombre = "",
            monto = BigDecimal.ZERO,
            fechaInicio = inicio,
            fechaFin = fin
        )
    }

    fun editarPresupuesto(presupuesto: Presupuesto) {
        errorFormulario = null
        presupuestoEnEdicion = presupuesto
    }

    fun cerrarFormulario() {
        presupuestoEnEdicion = null
        errorFormulario = null
    }

    fun guardar(
        nombre: String,
        montoTexto: String,
        periodo: TipoPeriodoPresupuesto,
        categoriaId: Long?,
        cuentaId: Long?,
        alertaPorcentaje: Int,
        fechaInicio: Long,
        fechaFin: Long?
    ) {
        viewModelScope.launch {
            if (nombre.isBlank()) {
                errorFormulario = "El nombre del presupuesto es obligatorio."
                return@launch
            }
            val monto = Utilidades.textoADinero(montoTexto)
            if (monto == null || monto.signum() <= 0) {
                errorFormulario = "Ingresa un monto mayor que cero."
                return@launch
            }
            val base = presupuestoEnEdicion
                ?: Presupuesto(nombre = nombre, monto = monto, fechaInicio = fechaInicio)
            try {
                repositorio.guardar(
                    base.copy(
                        nombre = nombre.trim(),
                        monto = monto,
                        periodo = periodo,
                        categoriaId = categoriaId,
                        cuentaId = cuentaId,
                        alertaPorcentaje = alertaPorcentaje.coerceIn(1, 100),
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        activo = true
                    )
                )
                presupuestoEnEdicion = null
                errorFormulario = null
            } catch (error: Exception) {
                errorFormulario = "No se pudo guardar el presupuesto."
            }
        }
    }

    fun eliminar(presupuesto: Presupuesto) {
        viewModelScope.launch { repositorio.eliminar(presupuesto) }
    }

    /** Rango de fechas sugerido para un periodo dado. */
    fun rangoPredeterminado(periodo: TipoPeriodoPresupuesto): Pair<Long, Long?> {
        val hoy = LocalDate.now()
        return when (periodo) {
            TipoPeriodoPresupuesto.SEMANAL -> {
                val inicio = Utilidades.milisDeFecha(hoy.minusDays(hoy.dayOfWeek.value.toLong() - 1))
                val fin = Utilidades.milisDeFecha(hoy.plusDays(7 - hoy.dayOfWeek.value.toLong())) + 86_399_999
                inicio to fin
            }
            TipoPeriodoPresupuesto.MENSUAL -> {
                val mes = YearMonth.from(hoy)
                Utilidades.milisDeFecha(mes.atDay(1)) to
                    Utilidades.milisDeFecha(mes.atEndOfMonth()) + 86_399_999
            }
            TipoPeriodoPresupuesto.ANUAL -> {
                Utilidades.milisDeFecha(LocalDate.of(hoy.year, 1, 1)) to
                    Utilidades.milisDeFecha(LocalDate.of(hoy.year, 12, 31)) + 86_399_999
            }
            TipoPeriodoPresupuesto.PERSONALIZADO -> {
                // Punto de partida: el mes en curso; el usuario ajusta las fechas.
                val mes = YearMonth.from(hoy)
                Utilidades.milisDeFecha(mes.atDay(1)) to
                    Utilidades.milisDeFecha(mes.atEndOfMonth()) + 86_399_999
            }
        }
    }
}
