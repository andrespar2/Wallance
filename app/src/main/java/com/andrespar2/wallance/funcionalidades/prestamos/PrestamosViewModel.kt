package com.andrespar2.wallance.funcionalidades.prestamos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.Pago
import com.andrespar2.wallance.nucleo.basedatos.entidades.PeriodicidadTasa
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.SistemaAmortizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoInteres
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPago
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.repositorios.CuentaConSaldo
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioContactos
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCuentas
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioPrestamos
import com.andrespar2.wallance.nucleo.finanzas.MotorPrestamos
import com.andrespar2.wallance.nucleo.finanzas.ResumenAmortizacion
import com.andrespar2.wallance.nucleo.finanzas.ResumenPrestamo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

/** Filtro de listado de préstamos por estado. */
enum class FiltroPrestamos { TODOS, OTORGADOS, RECIBIDOS, ACTIVOS, VENCIDOS, PAGADOS }

/** Estado de la pantalla de préstamos. */
data class EstadoPrestamos(
    val cargando: Boolean = true,
    val prestamos: List<ItemPrestamo> = emptyList(),
    val contactos: List<Contacto> = emptyList(),
    val moneda: String = "COP"
)

/** Préstamo junto a su resumen financiero y contacto. */
data class ItemPrestamo(
    val prestamo: Prestamo,
    val resumen: ResumenPrestamo,
    val contacto: Contacto?
)

@HiltViewModel
class PrestamosViewModel @Inject constructor(
    private val repositorio: RepositorioPrestamos,
    private val contactos: RepositorioContactos,
    cuentasRepo: RepositorioCuentas
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroPrestamos.TODOS)

    val filtroActual: StateFlow<FiltroPrestamos> = filtro

    /**
     * Contador que se incrementa cada vez que se crea un préstamo, para que la
     * pantalla desplace la lista al inicio y el nuevo préstamo quede visible
     * (la lista se ordena del más reciente al más antiguo).
     */
    var desplazarAlInicio by mutableStateOf(0)
        private set

    val cuentas: StateFlow<List<CuentaConSaldo>> = cuentasRepo.cuentasActivas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val estado: StateFlow<EstadoPrestamos> = combine(
        filtro,
        contactos.todos(),
        repositorio.todos()
    ) { tipoFiltro, listaContactos, listaPrestamos ->
        val items = listaPrestamos.map { prestamo ->
            ItemPrestamo(
                prestamo = prestamo,
                resumen = repositorio.resumen(prestamo),
                contacto = listaContactos.firstOrNull { it.id == prestamo.contactoId }
            )
        }.filter { item ->
            when (tipoFiltro) {
                FiltroPrestamos.TODOS -> true
                FiltroPrestamos.OTORGADOS -> item.prestamo.tipo == TipoPrestamo.OTORGADO
                FiltroPrestamos.RECIBIDOS -> item.prestamo.tipo == TipoPrestamo.RECIBIDO
                FiltroPrestamos.ACTIVOS -> item.prestamo.estado == EstadoPrestamo.ACTIVO ||
                    item.prestamo.estado == EstadoPrestamo.VENCIDO
                FiltroPrestamos.VENCIDOS -> item.prestamo.estado == EstadoPrestamo.VENCIDO
                FiltroPrestamos.PAGADOS -> item.prestamo.estado == EstadoPrestamo.PAGADO
            }
        }
        EstadoPrestamos(cargando = false, prestamos = items, contactos = listaContactos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPrestamos())

    fun cambiarFiltro(nuevo: FiltroPrestamos) {
        filtro.value = nuevo
    }

    // ------------------------------------------------------------------
    // Detalle de préstamo
    // ------------------------------------------------------------------

    var prestamoDetalle by mutableStateOf<Prestamo?>(null)

    var cuotasDetalle by mutableStateOf<List<Cuota>>(emptyList())

    var pagosDetalle by mutableStateOf<List<Pago>>(emptyList())

    var errorPago by mutableStateOf<String?>(null)

    fun abrirDetalle(prestamo: Prestamo) {
        prestamoDetalle = prestamo
        errorPago = null
        viewModelScope.launch {
            cuotasDetalle = repositorio.cuotasDeSuspend(prestamo.id)
            pagosDetalle = repositorio.pagosDeSuspend(prestamo.id)
        }
    }

    fun cerrarDetalle() {
        prestamoDetalle = null
        cuotasDetalle = emptyList()
        pagosDetalle = emptyList()
        errorPago = null
    }

    fun registrarPago(montoTexto: String, cuentaId: Long?, tipo: TipoPago) {
        val prestamo = prestamoDetalle ?: return
        viewModelScope.launch {
            val monto = com.andrespar2.wallance.nucleo.comun.Utilidades.textoADinero(montoTexto)
            if (monto == null || monto.signum() <= 0) {
                errorPago = "El monto del pago debe ser mayor que cero."
                return@launch
            }
            try {
                repositorio.registrarPago(
                    prestamoId = prestamo.id,
                    monto = monto,
                    tipo = tipo,
                    cuentaId = cuentaId,
                    prestamo = prestamo
                )
                cuotasDetalle = repositorio.cuotasDeSuspend(prestamo.id)
                pagosDetalle = repositorio.pagosDeSuspend(prestamo.id)
                prestamoDetalle = repositorio.prestamoPorId(prestamo.id)
                errorPago = null
            } catch (error: Exception) {
                errorPago = error.message ?: "No se pudo registrar el pago."
            }
        }
    }

    fun cancelarPrestamo(prestamo: Prestamo) {
        viewModelScope.launch {
            repositorio.cancelarPrestamo(prestamo.id)
            if (prestamoDetalle?.id == prestamo.id) prestamoDetalle = repositorio.prestamoPorId(prestamo.id)
        }
    }

    fun eliminarPrestamo(prestamo: Prestamo) {
        viewModelScope.launch {
            if (prestamoDetalle?.id == prestamo.id) cerrarDetalle()
            repositorio.eliminarPrestamo(prestamo.id)
        }
    }

    // ------------------------------------------------------------------
    // Contactos
    // ------------------------------------------------------------------

    var contactoEnEdicion by mutableStateOf<Contacto?>(null)
        private set

    var errorContacto by mutableStateOf<String?>(null)
        private set

    fun nuevoContacto() {
        errorContacto = null
        contactoEnEdicion = Contacto(nombre = "")
    }

    fun editarContacto(contacto: Contacto) {
        errorContacto = null
        contactoEnEdicion = contacto
    }

    fun cerrarContacto() {
        contactoEnEdicion = null
        errorContacto = null
    }

    fun guardarContacto(nombre: String, apellido: String, telefono: String, correo: String, notas: String) {
        viewModelScope.launch {
            if (nombre.isBlank()) {
                errorContacto = "El nombre del contacto es obligatorio."
                return@launch
            }
            val base = contactoEnEdicion ?: Contacto(nombre = nombre)
            try {
                contactos.guardar(
                    base.copy(
                        nombre = nombre.trim(),
                        apellido = apellido.trim(),
                        telefono = telefono.trim(),
                        correo = correo.trim(),
                        notas = notas.trim()
                    )
                )
                contactoEnEdicion = null
                errorContacto = null
            } catch (error: Exception) {
                errorContacto = "No se pudo guardar el contacto."
            }
        }
    }

    fun eliminarContacto(contacto: Contacto) {
        viewModelScope.launch {
            try {
                contactos.eliminar(contacto)
            } catch (error: Exception) {
                errorContacto = error.message
            }
        }
    }

    // ------------------------------------------------------------------
    // Préstamos: vista previa de cronograma y formulario
    // ------------------------------------------------------------------

    var prestamoEnEdicion by mutableStateOf<Prestamo?>(null)
        private set

    var vistaPrevia by mutableStateOf<ResumenAmortizacion?>(null)
        private set

    var errorFormulario by mutableStateOf<String?>(null)
        private set

    fun nuevoPrestamo(contactoId: Long, tipo: TipoPrestamo, moneda: String) {
        errorFormulario = null
        vistaPrevia = null
        val hoy = com.andrespar2.wallance.nucleo.comun.Utilidades.milisDeFecha(LocalDate.now())
        val unMes = com.andrespar2.wallance.nucleo.comun.Utilidades.milisDeFecha(LocalDate.now().plusMonths(1))
        prestamoEnEdicion = Prestamo(
            contactoId = contactoId,
            tipo = tipo,
            capital = BigDecimal.ZERO,
            moneda = moneda,
            tasaTipo = TipoInteres.SIN_INTERES,
            tasaValor = BigDecimal.ZERO,
            tasaPeriodicidad = PeriodicidadTasa.MENSUAL,
            sistemaAmortizacion = SistemaAmortizacion.PAGO_UNICO,
            numeroCuotas = 1,
            frecuenciaCuotas = FrecuenciaCuota.UNICA,
            fechaDesembolso = hoy,
            fechaVencimiento = unMes
        )
    }

    fun editarPrestamo(prestamo: Prestamo) {
        errorFormulario = null
        vistaPrevia = null
        prestamoEnEdicion = prestamo
    }

    fun cerrarFormulario() {
        prestamoEnEdicion = null
        vistaPrevia = null
        errorFormulario = null
    }

    fun actualizarPrestamoEnEdicion(transformacion: (Prestamo) -> Prestamo) {
        val actual = prestamoEnEdicion ?: return
        prestamoEnEdicion = transformacion(actual)
        recalcularVistaPrevia()
    }

    private fun recalcularVistaPrevia() {
        val prestamo = prestamoEnEdicion ?: return
        vistaPrevia = try {
            if (prestamo.capital.signum() <= 0) null
            else MotorPrestamos.calcularCronograma(prestamo)
        } catch (error: Exception) {
            null
        }
    }

    fun guardarPrestamo() {
        val prestamo = prestamoEnEdicion ?: return
        viewModelScope.launch {
            if (prestamo.capital.signum() <= 0) {
                errorFormulario = "El capital debe ser mayor que cero."
                return@launch
            }
            if (prestamo.numeroCuotas < 1) {
                errorFormulario = "Debe existir al menos una cuota."
                return@launch
            }
            try {
                repositorio.crearPrestamo(prestamo)
                desplazarAlInicio++
                prestamoEnEdicion = null
                vistaPrevia = null
                errorFormulario = null
            } catch (error: Exception) {
                errorFormulario = error.message ?: "No se pudo guardar el préstamo."
            }
        }
    }
}
