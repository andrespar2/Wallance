package com.andrespar2.wallance.funcionalidades.movimientos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioContactos
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioDivisas
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioMovimientos
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.esManual
import com.andrespar2.wallance.nucleo.basedatos.entidades.esGasto
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Estado visible de la lista de movimientos.
 */
data class EstadoListaMovimientos(
    val cargando: Boolean = true,
    val movimientos: List<Movimiento> = emptyList(),
    val cuentas: Map<Long, Cuenta> = emptyMap(),
    val categorias: Map<Long, Categoria> = emptyMap()
)

/** Controlador de la lista de movimientos con búsqueda y filtros. */
@HiltViewModel
class MovimientosViewModel @Inject constructor(
    private val repoMovimientos: RepositorioMovimientos,
    private val repoCuentas: RepositorioCuentas,
    private val repoCategorias: RepositorioCategorias
) : ViewModel() {

    /** Texto de búsqueda escrito por la persona usuaria. */
    var busqueda by mutableStateOf("")

    /** Filtro por tipo de movimiento; null significa "todos". */
    var tipoFiltro by mutableStateOf<TipoMovimiento?>(null)
        private set

    /** Cuando está activo, solo se muestran movimientos ligados a préstamos. */
    var soloPrestamos by mutableStateOf(false)
        private set

    val estado: StateFlow<EstadoListaMovimientos> =
        combine(
            combine(repoMovimientos.todos(), repoCuentas.todasLasCuentas()) { movimientos, cuentas ->
                movimientos to cuentas
            },
            repoCategorias.todas(),
            snapshotFlow { Triple(busqueda.trim(), tipoFiltro, soloPrestamos) }
        ) { (movimientos, cuentas), categorias, (texto, tipo, soloPrestamo) ->
            val filtrados = movimientos.filter { movimiento ->
                val coincideTexto = texto.isBlank() ||
                    movimiento.descripcion.contains(texto, ignoreCase = true) ||
                    movimiento.etiquetas.contains(texto, ignoreCase = true) ||
                    movimiento.notas.contains(texto, ignoreCase = true)
                val coincideTipo = tipo == null || movimiento.tipo == tipo
                val coincidePrestamo = !soloPrestamo || movimiento.prestamoId != null
                coincideTexto && coincideTipo && coincidePrestamo
            }
            EstadoListaMovimientos(
                cargando = false,
                movimientos = filtrados,
                cuentas = cuentas.associate { it.cuenta.id to it.cuenta },
                categorias = categorias.associateBy { it.id }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoListaMovimientos()
        )

    fun buscar(texto: String) {
        busqueda = texto
    }

    fun filtrarTipo(tipo: TipoMovimiento?) {
        tipoFiltro = if (tipoFiltro == tipo) null else tipo
    }

    fun alternarSoloPrestamos() {
        soloPrestamos = !soloPrestamos
    }

    fun limpiarFiltros() {
        busqueda = ""
        tipoFiltro = null
        soloPrestamos = false
    }

    /** Un movimiento originado por un préstamo no se elimina desde aquí. */
    fun eliminable(movimiento: Movimiento): Boolean = movimiento.prestamoId == null

    fun eliminarMovimiento(id: Long) {
        viewModelScope.launch { repoMovimientos.eliminar(id) }
    }
}

/**
 * Estado del formulario de movimientos.
 */
data class EstadoFormularioMovimiento(
    val cuentas: List<Cuenta> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val contactos: List<Contacto> = emptyList()
)

/**
 * Controlador del formulario de movimientos: crear, editar y duplicar.
 */
@HiltViewModel
class FormularioMovimientoViewModel @Inject constructor(
    private val repoMovimientos: RepositorioMovimientos,
    private val repoCuentas: RepositorioCuentas,
    private val repoCategorias: RepositorioCategorias,
    private val repoContactos: RepositorioContactos,
    private val repoDivisas: RepositorioDivisas,
    private val preferencias: com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
) : ViewModel() {

    var cargando by mutableStateOf(false)
        private set
    var inicializando by mutableStateOf(true)
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set

    var tipo by mutableStateOf(TipoMovimiento.GASTO)
        private set
    var montoTexto by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var notas by mutableStateOf("")
    var etiquetas by mutableStateOf("")
    var moneda by mutableStateOf("COP")
        private set
    var fecha by mutableStateOf(Utilidades.inicioDelDia())
        private set
    var ajusteEsAumento by mutableStateOf(true)
    var cuentaSeleccionadaId by mutableStateOf<Long?>(null)
    var cuentaDestinoId by mutableStateOf<Long?>(null)
    var categoriaSeleccionadaId by mutableStateOf<Long?>(null)
    var contactoId by mutableStateOf<Long?>(null)

    var estado by mutableStateOf(EstadoFormularioMovimiento())
        private set

    /** Moneda principal de la aplicación. */
    var monedaPrincipal by mutableStateOf("COP")
        private set

    /** Tasa de cambio vigente entre [moneda] y la moneda principal. */
    var tasaVista by mutableStateOf<java.math.BigDecimal?>(null)
        private set

    private var movimientoOriginalId: Long? = null

    /** Indica si el formulario está editando un movimiento existente. */
    val editando: Boolean get() = movimientoOriginalId != null

    init {
        viewModelScope.launch {
            monedaPrincipal = preferencias.monedaPrincipal.first()
            moneda = monedaPrincipal
            estado = estado.copy(
                cuentas = repoCuentas.todasSuspend(),
                contactos = repoContactos.todosSuspend()
            )
            if (cuentaSeleccionadaId == null) {
                cuentaSeleccionadaId = estado.cuentas.firstOrNull()?.id
            }
            cargarCategorias()
            inicializando = false
        }
    }

    fun cambiarTipo(nuevoTipo: TipoMovimiento) {
        tipo = nuevoTipo
        categoriaSeleccionadaId = null
        if (nuevoTipo != TipoMovimiento.TRANSFERENCIA) cuentaDestinoId = null
        viewModelScope.launch { cargarCategorias() }
    }

    fun cambiarMoneda(codigo: String) {
        moneda = codigo
        actualizarTasaVista()
    }

    /** Recalcula la tasa mostrada frente a la moneda principal. */
    fun actualizarTasaVista() {
        viewModelScope.launch {
            tasaVista = if (moneda == monedaPrincipal) null
            else repoDivisas.tasa(moneda, monedaPrincipal)
        }
    }

    fun cambiarFecha(milis: Long) {
        fecha = Utilidades.inicioDelDia(milis)
    }

    private suspend fun cargarCategorias() {
        val esperado = when {
            tipo.esIngreso -> TipoCategoria.INGRESO
            tipo.esGasto -> TipoCategoria.GASTO
            else -> null
        }
        val categorias = if (esperado == null) emptyList()
        else repoCategorias.activasPorTipo(esperado).first()
        estado = estado.copy(categorias = categorias)
    }

    /** Carga un movimiento existente para editarlo. */
    fun cargarMovimiento(id: Long) {
        viewModelScope.launch {
            val movimiento = repoMovimientos.porId(id) ?: return@launch
            movimientoOriginalId = movimiento.id
            aplicar(movimiento, movimiento.fecha)
        }
    }

    /** Carga un movimiento como base para duplicarlo con la fecha de hoy. */
    fun duplicar(id: Long) {
        viewModelScope.launch {
            val movimiento = repoMovimientos.porId(id) ?: return@launch
            movimientoOriginalId = null
            aplicar(movimiento, Utilidades.inicioDelDia())
        }
    }

    private suspend fun aplicar(movimiento: Movimiento, nuevaFecha: Long) {
        tipo = movimiento.tipo
        montoTexto = movimiento.monto.stripTrailingZeros().toPlainString()
        moneda = movimiento.moneda
        cuentaSeleccionadaId = movimiento.cuentaId
        cuentaDestinoId = movimiento.cuentaDestinoId
        categoriaSeleccionadaId = movimiento.categoriaId
        descripcion = movimiento.descripcion
        notas = movimiento.notas
        etiquetas = movimiento.etiquetas
        contactoId = movimiento.contactoId
        ajusteEsAumento = movimiento.ajusteEsAumento
        fecha = Utilidades.inicioDelDia(nuevaFecha)
        cargarCategorias()
        actualizarTasaVista()
    }

    fun establecerFechaNueva(nuevaFecha: Long) {
        fecha = Utilidades.inicioDelDia(nuevaFecha)
    }

    fun guardar(onGuardado: () -> Unit) {
        val monto = Utilidades.textoADinero(montoTexto)
        when {
            monto == null || monto.signum() <= 0 -> {
                mensajeError = "Ingresa un monto mayor que cero."
                return
            }
            cuentaSeleccionadaId == null -> {
                mensajeError = "Selecciona una cuenta."
                return
            }
            tipo == TipoMovimiento.TRANSFERENCIA && cuentaDestinoId == null -> {
                mensajeError = "Selecciona la cuenta de destino."
                return
            }
        }

        cargando = true
        mensajeError = null
        viewModelScope.launch {
            try {
                val principal = preferencias.monedaPrincipal.first()
                val tasa = if (moneda != principal) repoDivisas.tasa(moneda, principal) else null
                val montoEnPrincipal = if (moneda == principal) monto
                else tasa?.let { monto.multiply(it).setScale(2, RoundingMode.HALF_UP) } ?: monto

                val movimiento = Movimiento(
                    id = movimientoOriginalId ?: 0L,
                    tipo = tipo,
                    monto = monto,
                    moneda = moneda,
                    tasaCambio = tasa,
                    montoEnPrincipal = montoEnPrincipal,
                    categoriaId = if (tipo.esManual) categoriaSeleccionadaId else null,
                    cuentaId = cuentaSeleccionadaId!!,
                    cuentaDestinoId = if (tipo == TipoMovimiento.TRANSFERENCIA) cuentaDestinoId else null,
                    ajusteEsAumento = ajusteEsAumento,
                    fecha = fecha,
                    descripcion = descripcion.trim(),
                    contactoId = contactoId,
                    notas = notas.trim(),
                    etiquetas = etiquetas.trim()
                )
                if (movimientoOriginalId == null) {
                    repoMovimientos.registrar(movimiento)
                } else {
                    repoMovimientos.editar(movimiento)
                }
                cargando = false
                onGuardado()
            } catch (error: Exception) {
                cargando = false
                mensajeError = error.message ?: "No fue posible guardar el movimiento."
            }
        }
    }
}

/** Convierte una fecha LocalDate a milisegundos. */
fun LocalDate.aMilisegundos(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Convierte un LocalDateTime a milisegundos. */
fun LocalDateTime.aMilisegundos(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
