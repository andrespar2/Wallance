package com.andrespar2.wallance.funcionalidades.movimientos

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.esGasto
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance

/**
 * Lista de movimientos con búsqueda, filtros por tipo y acciones
 * de editar, duplicar y eliminar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMovimientos(
    onCrearMovimiento: () -> Unit,
    onEditarMovimiento: (Long) -> Unit,
    onDuplicarMovimiento: (Long) -> Unit,
    viewModel: MovimientosViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var movimientoAMenu by remember { mutableStateOf<Long?>(null) }
    var movimientoAEliminar by remember { mutableStateOf<Movimiento?>(null) }
    val monedaPrincipal = "COP"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Movimientos",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColoresWallance.TextoPrincipal
                )
                Text(
                    text = if (viewModel.soloPrestamos) "Ver todos" else "Solo préstamos",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.AmarilloPrincipal,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.alternarSoloPrestamos() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.busqueda,
                onValueChange = viewModel::buscar,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Buscar por descripción o etiqueta") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColoresWallance.AmarilloPrincipal,
                    unfocusedBorderColor = ColoresWallance.BordeSutil,
                    focusedTextColor = ColoresWallance.TextoPrincipal,
                    unfocusedTextColor = ColoresWallance.TextoPrincipal,
                    cursorColor = ColoresWallance.AmarilloPrincipal,
                    focusedContainerColor = ColoresWallance.Tarjeta,
                    unfocusedContainerColor = ColoresWallance.Tarjeta
                )
            )
        }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ChipFiltro(
                        texto = "Todos",
                        activo = viewModel.tipoFiltro == null,
                        onClick = { viewModel.limpiarFiltros() }
                    )
                }
                items(TiposFiltrables) { tipo ->
                    ChipFiltro(
                        texto = tipo.etiqueta,
                        activo = viewModel.tipoFiltro == tipo,
                        onClick = { viewModel.filtrarTipo(tipo) }
                    )
                }
            }
        }

        if (estado.cargando) {
            item { Cargando(modifier = Modifier.fillMaxWidth()) }
        } else if (estado.movimientos.isEmpty()) {
            item {
                EstadoVacio(
                    titulo = "Sin movimientos",
                    mensaje = if (viewModel.busqueda.isBlank()) {
                        "Registra tu primer ingreso o gasto con el botón +."
                    } else {
                        "No se encontraron movimientos que coincidan con la búsqueda."
                    },
                    icono = Icons.Default.Search,
                    textoAccion = if (viewModel.busqueda.isNotBlank()) "Limpiar búsqueda" else null,
                    onAccion = if (viewModel.busqueda.isNotBlank()) {
                        { viewModel.limpiarFiltros() }
                    } else null
                )
            }
        } else {
            items(estado.movimientos, key = { it.id }) { movimiento ->
                FilaMovimiento(
                    movimiento = movimiento,
                    nombreCuenta = estado.cuentas[movimiento.cuentaId]?.nombre.orEmpty(),
                    nombreCategoria = movimiento.categoriaId
                        ?.let { estado.categorias[it]?.nombre }
                        .orEmpty(),
                    moneda = movimiento.moneda.ifBlank { monedaPrincipal },
                    menuAbierto = movimientoAMenu == movimiento.id,
                    onAbrirMenu = { movimientoAMenu = movimiento.id },
                    onCerrarMenu = { movimientoAMenu = null },
                    onEditar = {
                        movimientoAMenu = null
                        onEditarMovimiento(movimiento.id)
                    },
                    onDuplicar = {
                        movimientoAMenu = null
                        onDuplicarMovimiento(movimiento.id)
                    },
                    onEliminar = {
                        movimientoAMenu = null
                        movimientoAEliminar = movimiento
                    }
                )
            }
        }
    }

    movimientoAEliminar?.let { movimiento ->
        ModalBottomSheet(
            onDismissRequest = { movimientoAEliminar = null },
            containerColor = ColoresWallance.SuperficieElevada
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = "¿Eliminar este movimiento?",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresWallance.TextoPrincipal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Se recalculará el saldo de la cuenta. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.TextoSecundario
                )
                Spacer(modifier = Modifier.height(20.dp))
                BotonPrincipal(
                    texto = "Sí, eliminar",
                    colorFondo = ColoresWallance.RojoPerdida,
                    colorTexto = ColoresWallance.TextoPrincipal,
                    onClick = {
                        viewModel.eliminarMovimiento(movimiento.id)
                        movimientoAEliminar = null
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BotonPrincipal(
                    texto = "Cancelar",
                    colorFondo = ColoresWallance.Tarjeta,
                    colorTexto = ColoresWallance.TextoPrincipal,
                    onClick = { movimientoAEliminar = null }
                )
            }
        }
    }
}

private val TiposFiltrables = listOf(
    TipoMovimiento.INGRESO,
    TipoMovimiento.GASTO,
    TipoMovimiento.TRANSFERENCIA,
    TipoMovimiento.AJUSTE
)

@Composable
private fun FilaMovimiento(
    movimiento: Movimiento,
    nombreCuenta: String,
    nombreCategoria: String,
    moneda: String,
    menuAbierto: Boolean,
    onAbrirMenu: () -> Unit,
    onCerrarMenu: () -> Unit,
    onEditar: () -> Unit,
    onDuplicar: () -> Unit,
    onEliminar: () -> Unit
) {
    val color = when {
        movimiento.tipo.esIngreso -> ColoresWallance.VerdeGanancia
        movimiento.tipo.esGasto -> ColoresWallance.RojoPerdida
        movimiento.tipo == TipoMovimiento.TRANSFERENCIA -> ColoresWallance.AmarilloPrincipal
        else -> ColoresWallance.TextoSecundario
    }
    val simbolo = when {
        movimiento.tipo.esIngreso -> "+"
        movimiento.tipo.esGasto -> "-"
        else -> ""
    }

    TarjetaWallance(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = movimiento.tipo.etiqueta.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movimiento.descripcion.ifBlank { movimiento.tipo.etiqueta },
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColoresWallance.TextoPrincipal,
                    maxLines = 1
                )
                val detalle = buildString {
                    if (nombreCategoria.isNotBlank()) append(nombreCategoria) else append(movimiento.tipo.etiqueta)
                    if (nombreCuenta.isNotBlank()) append(" · ").append(nombreCuenta)
                    append(" · ").append(Utilidades.formatearFechaCorta(movimiento.fecha))
                }
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoSecundario,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$simbolo${Utilidades.formatearDinero(movimiento.monto, moneda)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Box {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColoresWallance.TextoSecundario,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onAbrirMenu)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = onCerrarMenu,
                    containerColor = ColoresWallance.SuperficieElevada
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = onEditar
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar") },
                        onClick = onDuplicar
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = ColoresWallance.RojoPerdida) },
                        onClick = onEliminar
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipFiltro(texto: String, activo: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (activo) ColoresWallance.AmarilloPrincipal else ColoresWallance.Tarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (activo) ColoresWallance.NegroAmoled else ColoresWallance.TextoSecundario
        )
    }
}
