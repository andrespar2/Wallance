package com.andrespar2.wallance.funcionalidades.categorias

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.Separator
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance

/**
 * Pantalla para administrar las categorías de ingreso y gasto.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PantallaCategorias(
    onVolver: () -> Unit,
    viewModel: CategoriasViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarArchivadas by remember { mutableStateOf(false) }
    val categoriaEnEdicion = viewModel.categoriaEnEdicion
    val errorFormulario = viewModel.errorFormulario

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Categorías", color = ColoresWallance.TextoPrincipal) },
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
                    Text(
                        text = if (mostrarArchivadas) "Ver activas" else "Ver archivadas",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.AmarilloPrincipal,
                        modifier = Modifier
                            .clickable { mostrarArchivadas = !mostrarArchivadas }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColoresWallance.NegroSecundario,
                    titleContentColor = ColoresWallance.TextoPrincipal
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.nuevaCategoria(TipoCategoria.GASTO) },
                containerColor = ColoresWallance.AmarilloPrincipal,
                contentColor = ColoresWallance.NegroAmoled
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva categoría")
            }
        }
    ) { relleno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
            if (estado.cargando) {
                Cargando(modifier = Modifier.fillMaxSize())
            } else {
                val ingresos = estado.ingresos.filter { it.archivada == mostrarArchivadas }
                val gastos = estado.gastos.filter { it.archivada == mostrarArchivadas }
                if (ingresos.isEmpty() && gastos.isEmpty()) {
                    EstadoVacio(
                        titulo = if (mostrarArchivadas) "Sin categorías archivadas" else "Sin categorías",
                        mensaje = if (mostrarArchivadas) {
                            "Las categorías que archives aparecerán aquí."
                        } else {
                            "Agrega categorías para clasificar tus ingresos y gastos."
                        },
                        icono = Icons.Default.Category
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (ingresos.isNotEmpty()) {
                            item { Encabezado("Ingresos") }
                            item {
                                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        ingresos.forEachIndexed { indice, categoria ->
                                            FilaCategoria(
                                                categoria = categoria,
                                                onEditar = { viewModel.editarCategoria(categoria) },
                                                onAlternar = {
                                                    if (categoria.archivada) {
                                                        viewModel.restaurarCategoria(categoria)
                                                    } else {
                                                        viewModel.archivarCategoria(categoria)
                                                    }
                                                }
                                            )
                                            if (indice != ingresos.lastIndex) Separator()
                                        }
                                    }
                                }
                            }
                        }
                        if (gastos.isNotEmpty()) {
                            item { Encabezado("Gastos") }
                            item {
                                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        gastos.forEachIndexed { indice, categoria ->
                                            FilaCategoria(
                                                categoria = categoria,
                                                onEditar = { viewModel.editarCategoria(categoria) },
                                                onAlternar = {
                                                    if (categoria.archivada) {
                                                        viewModel.restaurarCategoria(categoria)
                                                    } else {
                                                        viewModel.archivarCategoria(categoria)
                                                    }
                                                }
                                            )
                                            if (indice != gastos.lastIndex) Separator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    categoriaEnEdicion?.let { categoria ->
        ModalFormularioCategoria(
            categoria = categoria,
            error = errorFormulario,
            onGuardar = { nombre, tipo, color ->
                viewModel.guardarCategoria(nombre, tipo, color)
            },
            onCancelar = viewModel::cerrarFormulario
        )
    }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = ColoresWallance.AmarilloPrincipal
    )
}

@Composable
private fun FilaCategoria(
    categoria: Categoria,
    onEditar: () -> Unit,
    onAlternar: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(categoria.color))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = categoria.nombre,
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresWallance.TextoPrincipal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Editar",
            style = MaterialTheme.typography.labelMedium,
            color = ColoresWallance.AmarilloPrincipal,
            modifier = Modifier
                .clickable(onClick = onEditar)
                .padding(6.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (categoria.archivada) "Restaurar" else "Archivar",
            style = MaterialTheme.typography.labelMedium,
            color = if (categoria.archivada) ColoresWallance.VerdeGanancia
            else ColoresWallance.RojoPerdida,
            modifier = Modifier
                .clickable(onClick = onAlternar)
                .padding(6.dp)
        )
    }
}

@Composable
private fun ModalFormularioCategoria(
    categoria: Categoria,
    error: String?,
    onGuardar: (String, TipoCategoria, Long) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(categoria.id) { mutableStateOf(categoria.nombre) }
    var tipo by remember(categoria.id) { mutableStateOf(categoria.tipo) }
    var color by remember(categoria.id) { mutableStateOf(categoria.color) }

    ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (categoria.id == 0L) "Nueva categoría" else "Editar categoría"
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            CampoTexto(valor = nombre, onValorCambiado = { nombre = it }, etiqueta = "Nombre")
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = tipo,
                opciones = TipoCategoria.values().toList(),
                etiqueta = "Tipo",
                textoDe = { it.etiqueta },
                onSeleccion = { tipo = it },
                textoValor = tipo.etiqueta
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelMedium,
                color = ColoresWallance.TextoSecundario
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColoresCategoria.paleta.forEach { tono ->
                    Box(
                        modifier = Modifier
                            .size(if (tono == color) 34.dp else 30.dp)
                            .clip(CircleShape)
                            .background(Color(tono))
                            .clickable { color = tono },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tono == color) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ColoresWallance.NegroAmoled)
                            )
                        }
                    }
                }
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = ColoresWallance.RojoPerdida,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            BotonPrincipal(texto = "Guardar", onClick = { onGuardar(nombre, tipo, color) })
            Spacer(modifier = Modifier.height(8.dp))
            BotonPrincipal(
                texto = "Cancelar",
                colorFondo = ColoresWallance.Tarjeta,
                colorTexto = ColoresWallance.TextoPrincipal,
                onClick = onCancelar
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
