package com.andrespar2.wallance.funcionalidades.prestamos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.Pago
import com.andrespar2.wallance.nucleo.basedatos.entidades.PeriodicidadTasa
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.SistemaAmortizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoInteres
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPago
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPrestamo
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPeligro
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonSecundario
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.DialogoConfirmacion
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.FilaDato
import com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo
import com.andrespar2.wallance.nucleo.diseno.componentes.Separator
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorFecha
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pantalla de préstamos: contactos, cronogramas, abonos y estados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrestamos(
    onVolver: () -> Unit,
    viewModel: PrestamosViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val filtro by viewModel.filtroActual.collectAsStateWithLifecycle()
    val cuentas by viewModel.cuentas.collectAsStateWithLifecycle()
    val listaState = rememberLazyListState()
    var contactoAEliminar by remember { mutableStateOf<Contacto?>(null) }
    var prestamoAEliminar by remember { mutableStateOf<Prestamo?>(null) }

    // Al crear un préstamo, desplaza la lista al inicio para que el nuevo quede visible.
    // Se reacciona tanto al contador (intención) como al cambio del primer elemento,
    // porque el nuevo préstamo se antepone y el estado llega de forma asíncrona.
    val primerPrestamoId = estado.prestamos.firstOrNull()?.prestamo?.id
    LaunchedEffect(viewModel.desplazarAlInicio, primerPrestamoId) {
        if (viewModel.desplazarAlInicio > 0) listaState.scrollToItem(0)
    }

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Préstamos", color = ColoresWallance.TextoPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = ColoresWallance.TextoPrincipal
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.nuevoContacto() }) {
                        Text("Contacto", color = ColoresWallance.AmarilloPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColoresWallance.NegroSecundario,
                    titleContentColor = ColoresWallance.TextoPrincipal
                )
            )
        },
        floatingActionButton = {
            if (estado.contactos.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        viewModel.nuevoPrestamo(
                            contactoId = estado.contactos.first().id,
                            tipo = TipoPrestamo.OTORGADO,
                            moneda = estado.moneda
                        )
                    },
                    containerColor = ColoresWallance.AmarilloPrincipal,
                    contentColor = ColoresWallance.NegroAmoled
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo préstamo")
                }
            }
        }
    ) { relleno ->
        Box(modifier = Modifier.fillMaxSize().padding(relleno)) {
            when {
                estado.cargando -> Cargando(modifier = Modifier.fillMaxSize())

                estado.contactos.isEmpty() -> EstadoVacio(
                    titulo = "Sin contactos",
                    mensaje = "Crea un contacto para empezar a registrar préstamos.",
                    icono = Icons.Default.Handshake,
                    textoAccion = "Crear contacto",
                    onAccion = { viewModel.nuevoContacto() }
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    FilaFiltros(filtro = filtro, onCambio = viewModel::cambiarFiltro)

                    if (estado.prestamos.isEmpty()) {
                        EstadoVacio(
                            titulo = "Sin préstamos",
                            mensaje = "Registra el dinero que prestas o recibes con intereses y cuotas.",
                            icono = Icons.Default.Handshake
                        )
                    } else {
                        LazyColumn(
                            state = listaState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(estado.prestamos, key = { it.prestamo.id }) { item ->
                                TarjetaPrestamo(
                                    item = item,
                                    onAbrir = { viewModel.abrirDetalle(item.prestamo) },
                                    onEditar = { viewModel.editarPrestamo(item.prestamo) },
                                    onEliminar = { prestamoAEliminar = item.prestamo }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    contactoAEliminar?.let { contacto ->
        DialogoConfirmacion(
            titulo = "¿Eliminar contacto?",
            mensaje = "Se eliminará el perfil de ${contacto.nombreCompleto}.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = {
                viewModel.eliminarContacto(contacto)
                contactoAEliminar = null
            },
            onCancelar = { contactoAEliminar = null }
        )
    }

    prestamoAEliminar?.let { prestamo ->
        DialogoConfirmacion(
            titulo = "¿Eliminar préstamo?",
            mensaje = "Se eliminarán el préstamo, sus cuotas y pagos. Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = {
                viewModel.eliminarPrestamo(prestamo)
                prestamoAEliminar = null
            },
            onCancelar = { prestamoAEliminar = null }
        )
    }

    viewModel.contactoEnEdicion?.let { contacto ->
        FormularioContacto(
            contacto = contacto,
            error = viewModel.errorContacto,
            onGuardar = viewModel::guardarContacto,
            onCancelar = viewModel::cerrarContacto
        )
    }

    viewModel.prestamoEnEdicion?.let { prestamo ->
        FormularioPrestamo(
            prestamo = prestamo,
            contacto = estado.contactos.firstOrNull { it.id == prestamo.contactoId },
            contactos = estado.contactos,
            vistaPrevia = viewModel.vistaPrevia,
            error = viewModel.errorFormulario,
            onCambio = viewModel::actualizarPrestamoEnEdicion,
            onContactoCambiado = { id ->
                viewModel.actualizarPrestamoEnEdicion { it.copy(contactoId = id) }
            },
            onGuardar = viewModel::guardarPrestamo,
            onCancelar = viewModel::cerrarFormulario
        )
    }

    viewModel.prestamoDetalle?.let { prestamo ->
        val item = estado.prestamos.firstOrNull { it.prestamo.id == prestamo.id }
        ModalDetallePrestamo(
            prestamo = prestamo,
            contacto = item?.contacto,
            cuotas = viewModel.cuotasDetalle,
            pagos = viewModel.pagosDetalle,
            cuentas = cuentas,
            error = viewModel.errorPago,
            onRegistrarPago = { monto, cuentaId, tipo ->
                viewModel.registrarPago(monto, cuentaId, tipo)
            },
            onCancelar = { viewModel.cancelarPrestamo(prestamo) },
            onCerrar = viewModel::cerrarDetalle
        )
    }
}

@Composable
private fun FilaFiltros(filtro: FiltroPrestamos, onCambio: (FiltroPrestamos) -> Unit) {
    val filtros = listOf(
        FiltroPrestamos.TODOS to "Todos",
        FiltroPrestamos.OTORGADOS to "Otorgados",
        FiltroPrestamos.RECIBIDOS to "Recibidos",
        FiltroPrestamos.ACTIVOS to "Activos",
        FiltroPrestamos.VENCIDOS to "Vencidos",
        FiltroPrestamos.PAGADOS to "Pagados"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filtros.forEach { (valor, texto) ->
            FilterChip(
                selected = filtro == valor,
                onClick = { onCambio(valor) },
                label = { Text(texto) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ColoresWallance.AmarilloPrincipal,
                    selectedLabelColor = ColoresWallance.NegroAmoled,
                    containerColor = ColoresWallance.Tarjeta,
                    labelColor = ColoresWallance.TextoSecundario
                )
            )
        }
    }
}
