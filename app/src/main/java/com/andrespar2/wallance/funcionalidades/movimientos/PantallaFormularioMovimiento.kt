package com.andrespar2.wallance.funcionalidades.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.esGasto
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.FilaDato
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorDesplegable
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas

/**
 * Formulario para registrar, editar o duplicar un movimiento.
 *
 * No muestra publicidad: se trata de una pantalla sensible con datos
 * financieros que la persona está introduciendo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioMovimiento(
    movimientoId: Long?,
    duplicar: Boolean,
    onVolver: () -> Unit,
    viewModel: FormularioMovimientoViewModel = hiltViewModel()
) {
    var mostrarSelectorFecha by remember { mutableStateOf(false) }

    LaunchedEffect(movimientoId, duplicar) {
        when {
            movimientoId == null -> Unit
            duplicar -> viewModel.duplicar(movimientoId)
            else -> viewModel.cargarMovimiento(movimientoId)
        }
    }

    val estado = viewModel.estado

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = ColoresWallance.TextoPrincipal
                )
            }
            Text(
                text = when {
                    duplicar -> "Duplicar movimiento"
                    viewModel.editando -> "Editar movimiento"
                    else -> "Nuevo movimiento"
                },
                style = MaterialTheme.typography.titleLarge,
                color = ColoresWallance.TextoPrincipal
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Tipo de movimiento.
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                TiposManuales.forEach { tipo ->
                    ChipTipo(
                        texto = tipo.etiqueta,
                        activo = viewModel.tipo == tipo,
                        onClick = { viewModel.cambiarTipo(tipo) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CampoTexto(
                valor = viewModel.montoTexto,
                onValorCambiado = { viewModel.montoTexto = it },
                etiqueta = "Monto (${viewModel.moneda})",
                esDinero = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorDesplegable(
                valor = viewModel.moneda,
                opciones = MotorDivisas.MONEDAS.map { it.first },
                etiqueta = "Moneda",
                textoDe = { codigo -> "$codigo · ${MotorDivisas.nombreDeMoneda(codigo)}" },
                onSeleccion = viewModel::cambiarMoneda
            )

            if (viewModel.moneda != viewModel.monedaPrincipal) {
                Spacer(modifier = Modifier.height(8.dp))
                val tasa = viewModel.tasaVista
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    if (tasa == null) {
                        Text(
                            text = "No hay cotización disponible sin conexión. El movimiento se guardará " +
                                "sin conversión hasta que actualices las divisas.",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColoresWallance.TextoSecundario
                        )
                    } else {
                        FilaDato(
                            etiqueta = "Tasa actual",
                            valor = "1 ${viewModel.moneda} = " +
                                "${tasa.stripTrailingZeros().toPlainString()} ${viewModel.monedaPrincipal}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CampoTexto(
                valor = viewModel.descripcion,
                onValorCambiado = { viewModel.descripcion = it },
                etiqueta = "Descripción"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectorDesplegable(
                valor = viewModel.cuentaSeleccionadaId,
                opciones = estado.cuentas.map { it.id },
                etiqueta = if (viewModel.tipo == TipoMovimiento.TRANSFERENCIA) "Cuenta de origen" else "Cuenta",
                textoDe = { id -> estado.cuentas.firstOrNull { it.id == id }?.nombre ?: "Selecciona una cuenta" },
                onSeleccion = { viewModel.cuentaSeleccionadaId = it }
            )

            if (viewModel.tipo == TipoMovimiento.TRANSFERENCIA) {
                Spacer(modifier = Modifier.height(12.dp))
                val destinos = estado.cuentas.filter { it.id != viewModel.cuentaSeleccionadaId }
                SelectorDesplegable(
                    valor = viewModel.cuentaDestinoId,
                    opciones = destinos.map { it.id },
                    etiqueta = "Cuenta de destino",
                    textoDe = { id -> destinos.firstOrNull { it.id == id }?.nombre ?: "Selecciona una cuenta" },
                    onSeleccion = { viewModel.cuentaDestinoId = it }
                )
            }

            if (viewModel.tipo.esIngreso || viewModel.tipo.esGasto) {
                Spacer(modifier = Modifier.height(12.dp))
                val etiquetaCategoria = estado.categorias.firstOrNull { it.id == viewModel.categoriaSeleccionadaId }?.nombre
                SelectorDesplegable(
                    valor = viewModel.categoriaSeleccionadaId,
                    opciones = estado.categorias.map { it.id },
                    etiqueta = "Categoría",
                    textoDe = { id -> estado.categorias.firstOrNull { it.id == id }?.nombre ?: "Sin categoría" },
                    onSeleccion = { viewModel.categoriaSeleccionadaId = it }
                )
                if (etiquetaCategoria == null && estado.categorias.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sin categoría asignada.",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                }
            }

            if (viewModel.tipo == TipoMovimiento.AJUSTE) {
                Spacer(modifier = Modifier.height(12.dp))
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (viewModel.ajusteEsAumento) "Aumenta el saldo" else "Disminuye el saldo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColoresWallance.TextoPrincipal,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = viewModel.ajusteEsAumento,
                            onCheckedChange = { viewModel.ajusteEsAumento = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ColoresWallance.NegroAmoled,
                                checkedTrackColor = ColoresWallance.AmarilloPrincipal
                            )
                        )
                    }
                }
            }

            // Fecha.
            Spacer(modifier = Modifier.height(12.dp))
            TarjetaWallance(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = ColoresWallance.AmarilloPrincipal
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            text = "Fecha del movimiento",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColoresWallance.TextoSecundario
                        )
                        Text(
                            text = Utilidades.formatearFecha(viewModel.fecha),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColoresWallance.TextoPrincipal
                        )
                    }
                    TextButton(onClick = { mostrarSelectorFecha = true }) {
                        Text("Cambiar", color = ColoresWallance.AmarilloPrincipal)
                    }
                }
            }

            // Contacto.
            if (estado.contactos.isNotEmpty() && (viewModel.tipo.esIngreso || viewModel.tipo.esGasto)) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectorDesplegable(
                    valor = viewModel.contactoId,
                    opciones = listOf<Long?>(null) + estado.contactos.map { it.id },
                    etiqueta = "Contacto (opcional)",
                    textoDe = { id -> textoContacto(id, estado.contactos) },
                    onSeleccion = { viewModel.contactoId = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CampoTexto(
                valor = viewModel.etiquetas,
                onValorCambiado = { viewModel.etiquetas = it },
                etiqueta = "Etiquetas (separadas por coma)"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CampoTexto(
                valor = viewModel.notas,
                onValorCambiado = { viewModel.notas = it },
                etiqueta = "Notas",
                unaLinea = false
            )

            viewModel.mensajeError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.RojoPerdida
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            BotonPrincipal(
                texto = if (viewModel.editando) "Guardar cambios" else "Registrar movimiento",
                onClick = { viewModel.guardar(onGuardado = onVolver) },
                habilitado = !viewModel.inicializando && estado.cuentas.isNotEmpty(),
                cargando = viewModel.cargando
            )

            if (estado.cuentas.isEmpty() && !viewModel.inicializando) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Necesitas al menos una cuenta para registrar movimientos.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.RojoPerdida
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (mostrarSelectorFecha) {
        val selector = rememberDatePickerState(initialSelectedDateMillis = viewModel.fecha)
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    selector.selectedDateMillis?.let(viewModel::cambiarFecha)
                    mostrarSelectorFecha = false
                }) {
                    Text("Aceptar", color = ColoresWallance.AmarilloPrincipal)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) {
                    Text("Cancelar", color = ColoresWallance.TextoSecundario)
                }
            }
        ) {
            DatePicker(state = selector)
        }
    }
}

private val TiposManuales = listOf(
    TipoMovimiento.GASTO,
    TipoMovimiento.INGRESO,
    TipoMovimiento.TRANSFERENCIA,
    TipoMovimiento.AJUSTE
)

@Composable
private fun ChipTipo(texto: String, activo: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (activo) ColoresWallance.AmarilloPrincipal else ColoresWallance.Tarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (activo) ColoresWallance.NegroAmoled else ColoresWallance.TextoSecundario
        )
    }
}

private fun nombreContacto(contacto: Contacto): String =
    contacto.nombreCompleto.ifBlank { contacto.nombre }

private fun textoContacto(id: Long?, contactos: List<Contacto>): String {
    if (id == null) return "Sin contacto"
    val contacto = contactos.firstOrNull { it.id == id } ?: return "Sin contacto"
    return nombreContacto(contacto)
}
