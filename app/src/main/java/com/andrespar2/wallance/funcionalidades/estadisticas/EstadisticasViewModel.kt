package com.andrespar2.wallance.funcionalidades.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.esGasto
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioMovimientos
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject

/** Periodo seleccionado para las estadísticas. */
enum class PeriodoEstadistica(val etiqueta: String, val meses: Int) {
    MES("Mes", 1),
    TRIMESTRE("Trimestre", 3),
    SEMESTRE("Semestre", 6),
    ANO("Año", 12)
}

/** Un mes del gráfico de evolución. */
data class PuntoEvolucion(
    val etiqueta: String,
    val ingresos: BigDecimal,
    val gastos: BigDecimal
)

/** Participación de una categoría dentro del total de gastos. */
data class ParteCategoria(
    val categoria: Categoria?,
    val monto: BigDecimal,
    val porcentaje: Double
)

/** Estado consolidado de la pantalla de estadísticas. */
data class EstadoEstadisticas(
    val cargando: Boolean = true,
    val moneda: String = "COP",
    val periodo: PeriodoEstadistica = PeriodoEstadistica.MES,
    val ingresos: BigDecimal = BigDecimal.ZERO,
    val gastos: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val evolucion: List<PuntoEvolucion> = emptyList(),
    val categorias: List<ParteCategoria> = emptyList()
)

/**
 * Controlador de estadísticas.
 *
 * Agrega los movimientos reales del usuario (sin datos ficticios) para
 * construir el balance del periodo, la evolución mensual y el desglose por
 * categoría. Todos los cálculos usan [BigDecimal] con redondeo HALF_UP.
 */
@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    movimientos: RepositorioMovimientos,
    categorias: RepositorioCategorias,
    preferencias: RepositorioPreferencias
) : ViewModel() {

    private val periodo = MutableStateFlow(PeriodoEstadistica.MES)

    val periodoActual: StateFlow<PeriodoEstadistica> = periodo

    val estado: StateFlow<EstadoEstadisticas> = combine(
        periodo,
        movimientos.todos(),
        categorias.todas(),
        preferencias.monedaPrincipal
    ) { actual, listaMovimientos, listaCategorias, moneda ->
        DatosCombinados(actual, listaMovimientos, listaCategorias, moneda)
    }.mapLatest { datos ->
        construirEstado(datos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoEstadisticas())

    fun cambiarPeriodo(nuevo: PeriodoEstadistica) {
        periodo.value = nuevo
    }

    private data class DatosCombinados(
        val periodo: PeriodoEstadistica,
        val movimientos: List<Movimiento>,
        val categorias: List<Categoria>,
        val moneda: String
    )

    private suspend fun construirEstado(datos: DatosCombinados): EstadoEstadisticas {
        val hoy = LocalDate.now()
        val inicioPeriodo = hoy.minusMonths((datos.periodo.meses - 1).toLong()).withDayOfMonth(1)
        val desde = UtilidadesRango.inicioDeMilis(inicioPeriodo)
        val hasta = UtilidadesRango.finDeMilis(hoy)

        val moneda = datos.moneda
        val relevantes = datos.movimientos.filter { it.fecha in desde..hasta }

        var ingresos = BigDecimal.ZERO
        var gastos = BigDecimal.ZERO
        val porCategoria = HashMap<Long?, BigDecimal>()

        for (movimiento in relevantes) {
            val monto = montoEnPrincipal(movimiento, moneda)
            when {
                movimiento.tipo.esIngreso -> ingresos = ingresos.add(monto)
                movimiento.tipo.esGasto -> {
                    gastos = gastos.add(monto)
                    val clave = movimiento.categoriaId
                    porCategoria[clave] = (porCategoria[clave] ?: BigDecimal.ZERO).add(monto)
                }
            }
        }

        val evolucion = construirEvolucion(datos.movimientos, moneda)
        val categorias = construirCategorias(porCategoria, datos.categorias, gastos)

        return EstadoEstadisticas(
            cargando = false,
            moneda = moneda,
            periodo = datos.periodo,
            ingresos = ingresos,
            gastos = gastos,
            balance = ingresos.subtract(gastos),
            evolucion = evolucion,
            categorias = categorias
        )
    }

    /** Construye los últimos 6 meses de ingresos frente a gastos. */
    private fun construirEvolucion(
        movimientos: List<Movimiento>,
        moneda: String
    ): List<PuntoEvolucion> {
        val hoy = LocalDate.now()
        val puntos = ArrayList<PuntoEvolucion>(6)
        for (atras in 5 downTo 0) {
            val mes = hoy.minusMonths(atras.toLong())
            val inicio = UtilidadesRango.inicioDeMilis(mes)
            val fin = UtilidadesRango.finDeMilis(mes)
            var ingresosMes = BigDecimal.ZERO
            var gastosMes = BigDecimal.ZERO
            for (movimiento in movimientos) {
                if (movimiento.fecha < inicio || movimiento.fecha > fin) continue
                val monto = montoEnPrincipal(movimiento, moneda)
                when {
                    movimiento.tipo.esIngreso -> ingresosMes = ingresosMes.add(monto)
                    movimiento.tipo.esGasto -> gastosMes = gastosMes.add(monto)
                }
            }
            puntos.add(
                PuntoEvolucion(
                    etiqueta = UtilidadesRango.etiquetaDeMes(mes),
                    ingresos = ingresosMes,
                    gastos = gastosMes
                )
            )
        }
        return puntos
    }

    /** Reparte los gastos por categoría y calcula su porcentaje. */
    private fun construirCategorias(
        porCategoria: Map<Long?, BigDecimal>,
        categorias: List<Categoria>,
        totalGastos: BigDecimal
    ): List<ParteCategoria> {
        if (totalGastos.signum() <= 0) return emptyList()
        return porCategoria.map { (id, monto) ->
            val categoria = categorias.firstOrNull { it.id == id }
            val porcentaje = monto
                .multiply(BigDecimal(100))
                .divide(totalGastos, 1, RoundingMode.HALF_UP)
                .toDouble()
            ParteCategoria(categoria = categoria, monto = monto, porcentaje = porcentaje)
        }.sortedByDescending { it.monto }
    }

    /** Equivalente del movimiento en la moneda principal. */
    private fun montoEnPrincipal(movimiento: Movimiento, moneda: String): BigDecimal =
        if (movimiento.moneda == moneda) movimiento.monto
        else movimiento.montoEnPrincipal ?: movimiento.monto
}

/** Utilidades de rango de fechas para las agregaciones. */
private object UtilidadesRango {
    fun inicioDeMilis(fecha: LocalDate): Long =
        com.andrespar2.wallance.nucleo.comun.Utilidades.milisDeFecha(fecha)

    fun finDeMilis(fecha: LocalDate): Long =
        com.andrespar2.wallance.nucleo.comun.Utilidades.milisDeFecha(fecha.plusDays(1)) - 1

    fun etiquetaDeMes(fecha: LocalDate): String =
        fecha.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale("es"))
            .replaceFirstChar { it.uppercase() }
}
