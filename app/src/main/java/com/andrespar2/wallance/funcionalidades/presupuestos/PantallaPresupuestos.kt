package com.andrespar2.wallance.funcionalidades.presupuestos

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.Presupuesto
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPeriodoPresupuesto
import com.andrespar2.wallance.nucleo.basedatos.repositorios.EstadoPresupuesto
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorFecha
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import java.math.BigDecimal

/**
 * Pantalla para definir y seguir presupuestos de gasto por categoría, cuenta
 * o de forma global.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PantallaPresupuestos(
    onVolver: () -> Unit,
    viewModel: PresupuestosViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var presupuestoAEliminar by remember { mutableStateOf<Presupuesto?>(null) }

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos", color = ColoresWallance.TextoPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = ColoresWallance.TextoPrincipal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColoresWallance.NegroSecundario,
                    titleContentColor = ColoresWallance.TextoPrincipal
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::nuevoPresupuesto,
                containerColor = ColoresWallance.AmarilloPrincipal,
                contentColor = ColoresWallance.NegroAmoled
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo presupuesto")
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
            } else if (estado.presupuestos.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin presupuestos",
                    mensaje = "Crea un presupuesto para controlar cuánto gastas cada periodo.",
                    icono = Icons.Default.TrackChanges
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.presupuestos, key = { it.presupuesto.id }) { item ->
                        TarjetaPresupuesto(
                            item = item,
                            estado = estado,
                            onEditar = { viewModel.editarPresupuesto(item.presupuesto) },
                            onEliminar = { presupuestoAEliminar = item.presupuesto }
                        )
                    }
                }
            }
        }
    }

    val enEdicion = viewModel.presupuestoEnEdicion
    if (enEdicion != null) {
        ModalFormularioPresupuesto(
            presupuesto = enEdicion,
            categorias = estado.categorias.values.toList(),
            cuentas = estado.cuentas.values.toList(),
            moneda = estado.moneda,
            error = viewModel.errorFormulario,
            onGuardar = { nombre, monto, periodo, categoriaId, cuentaId, alerta, inicio, fin ->
                viewModel.guardar(nombre, monto, periodo, categoriaId, cuentaId, alerta, inicio, fin)
            },
            rangoDe = viewModel::rangoPredeterminado,
            onCancelar = viewModel::cerrarFormulario
        )
    }

    presupuestoAEliminar?.let { presupuesto ->
        com.andrespar2.wallance.nucleo.diseno.componentes.DialogoConfirmacion(
            titulo = "¿Eliminar este presupuesto?",
            mensaje = "El presupuesto dejará de aparecer. Los movimientos no se modifican.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = {
                viewModel.eliminar(presupuesto)
                presupuestoAEliminar = null
            },
            onCancelar = { presupuestoAEliminar = null }
        )
    }
}

@Composable
private fun TarjetaPresupuesto(
    item: EstadoPresupuesto,
    estado: EstadoPresupuestos,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val presupuesto = item.presupuesto
    val alcance = when {
        presupuesto.categoriaId != null ->
            estado.categorias[presupuesto.categoriaId]?.nombre ?: "Categoría"
        presupuesto.cuentaId != null ->
            estado.cuentas[presupuesto.cuentaId]?.nombre ?: "Cuenta"
        else -> "Todos los gastos"
    }
    val fraccion = (item.porcentajeConsumido.toFloat() / 100f).coerceIn(0f, 1f)
    val colorBarra = when {
        item.porcentajeConsumido >= BigDecimal(100) -> ColoresWallance.RojoPerdida
        item.porcentajeConsumido >= BigDecimal(presupuesto.alertaPorcentaje) ->
            ColoresWallance.AmarilloPrincipal
        else -> ColoresWallance.VerdeGanancia
    }

    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presupuesto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = ColoresWallance.TextoPrincipal
                    )
                    Text(
                        text = "$alcance · ${presupuesto.periodo.etiqueta}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { fraccion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colorBarra,
                trackColor = ColoresWallance.SuperficieElevada
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Gastado ${Utilidades.formatearDinero(item.gastado, presupuesto.moneda)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "de ${Utilidades.formatearDinero(presupuesto.monto, presupuesto.moneda)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoPrincipal,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (item.disponible.signum() >= 0) {
                        "Disponible ${Utilidades.formatearDinero(item.disponible, presupuesto.moneda)}"
                    } else {
                        "Excedido ${Utilidades.formatearDinero(item.disponible.abs(), presupuesto.moneda)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorBarra,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    Text(
                        text = "Editar",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.AmarilloPrincipal,
                        modifier = Modifier.clickable(onClick = onEditar).padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Eliminar",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.RojoPerdida,
                        modifier = Modifier.clickable(onClick = onEliminar).padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModalFormularioPresupuesto(
    presupuesto: Presupuesto,
    categorias: List<Categoria>,
    cuentas: List<Cuenta>,
    moneda: String,
    error: String?,
    onGuardar: (
        String, String, TipoPeriodoPresupuesto, Long?, Long?, Int, Long, Long?
    ) -> Unit,
    rangoDe: (TipoPeriodoPresupuesto) -> Pair<Long, Long?>,
    onCancelar: () -> Unit
) {
    var nombre by remember(presupuesto.id) { mutableStateOf(presupuesto.nombre) }
    var monto by remember(presupuesto.id) {
        mutableStateOf(
            if (presupuesto.id == 0L) ""
            else presupuesto.monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        )
    }
    var periodo by remember(presupuesto.id) { mutableStateOf(presupuesto.periodo) }
    var categoriaSel by remember(presupuesto.id) {
        mutableStateOf(categorias.firstOrNull { it.id == presupuesto.categoriaId })
    }
    var cuentaSel by remember(presupuesto.id) {
        mutableStateOf(cuentas.firstOrNull { it.id == presupuesto.cuentaId })
    }
    var alerta by remember(presupuesto.id) { mutableStateOf(presupuesto.alertaPorcentaje.toString()) }
    var fechaInicio by remember(presupuesto.id) { mutableStateOf(presupuesto.fechaInicio) }
    var fechaFin by remember(presupuesto.id) {
        mutableStateOf(presupuesto.fechaFin ?: presupuesto.fechaInicio)
    }

    ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (presupuesto.id == 0L) "Nuevo presupuesto" else "Editar presupuesto"
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            CampoTexto(valor = nombre, onValorCambiado = { nombre = it }, etiqueta = "Nombre")
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = monto,
                onValorCambiado = { monto = it },
                etiqueta = "Monto (${moneda})",
                esDinero = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = periodo,
                opciones = TipoPeriodoPresupuesto.values().toList(),
                etiqueta = "Periodo",
                textoDe = { it.etiqueta },
                onSeleccion = { nuevo ->
                    periodo = nuevo
                    val (inicio, fin) = rangoDe(nuevo)
                    fechaInicio = inicio
                    fin?.let { fechaFin = it }
                },
                textoValor = periodo.etiqueta
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = categoriaSel,
                opciones = listOf<Categoria?>(null) + categorias,
                etiqueta = "Categoría",
                textoDe = { it?.nombre ?: "Todas las categorías" },
                onSeleccion = { categoriaSel = it },
                textoValor = categoriaSel?.nombre ?: "Todas las categorías"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = cuentaSel,
                opciones = listOf<Cuenta?>(null) + cuentas,
                etiqueta = "Cuenta",
                textoDe = { it?.nombre ?: "Todas las cuentas" },
                onSeleccion = { cuentaSel = it },
                textoValor = cuentaSel?.nombre ?: "Todas las cuentas"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorFecha(
                valor = fechaInicio,
                onSeleccion = { fechaInicio = it },
                etiqueta = "Inicio del periodo"
            )
            if (periodo == TipoPeriodoPresupuesto.PERSONALIZADO) {
                Spacer(modifier = Modifier.height(10.dp))
                SelectorFecha(
                    valor = fechaFin,
                    onSeleccion = { fechaFin = it },
                    etiqueta = "Fin del periodo"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = alerta,
                onValorCambiado = { alerta = it.filter { caracter -> caracter.isDigit() }.take(3) },
                etiqueta = "Alerta al consumir (%)",
                ayuda = "Se resalta la barra al superar este porcentaje."
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = ColoresWallance.RojoPerdida,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            BotonPrincipal(
                texto = "Guardar",
                onClick = {
                    onGuardar(
                        nombre,
                        monto,
                        periodo,
                        categoriaSel?.id,
                        cuentaSel?.id,
                        alerta.toIntOrNull() ?: 80,
                        fechaInicio,
                        if (periodo == TipoPeriodoPresupuesto.PERSONALIZADO) fechaFin else null
                    )
                }
            )
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
