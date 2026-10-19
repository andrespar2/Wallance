package com.andrespar2.wallance.funcionalidades.ahorros

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.MetaAhorro
import com.andrespar2.wallance.nucleo.basedatos.repositorios.EstadoMeta
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorFecha
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.Separator
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import java.math.BigDecimal

/**
 * Pantalla de metas de ahorro: creación, seguimiento de aportes y progreso.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PantallaAhorros(
    onVolver: () -> Unit,
    viewModel: AhorrosViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var metaAEliminar by remember { mutableStateOf<MetaAhorro?>(null) }

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Metas de ahorro", color = ColoresWallance.TextoPrincipal) },
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
                onClick = { viewModel.nuevaMeta(estado.moneda) },
                containerColor = ColoresWallance.AmarilloPrincipal,
                contentColor = ColoresWallance.NegroAmoled
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva meta")
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
            } else if (estado.metas.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin metas de ahorro",
                    mensaje = "Define una meta y registra tus aportes para alcanzarla.",
                    icono = Icons.Default.Savings
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.metas, key = { it.meta.id }) { item ->
                        TarjetaMeta(
                            item = item,
                            onAbrir = { viewModel.abrirDetalle(item.meta) },
                            onEditar = { viewModel.editarMeta(item.meta) },
                            onEliminar = { metaAEliminar = item.meta }
                        )
                    }
                }
            }
        }
    }

    val enEdicion = viewModel.metaEnEdicion
    if (enEdicion != null) {
        ModalFormularioMeta(
            meta = enEdicion,
            cuentas = estado.cuentas,
            moneda = estado.moneda,
            error = viewModel.errorFormulario,
            onGuardar = { nombre, monto, moneda, fecha, cuentaId ->
                viewModel.guardar(nombre, monto, moneda, fecha, cuentaId)
            },
            onCancelar = viewModel::cerrarFormulario
        )
    }

    val detalle = viewModel.metaDetalle
    if (detalle != null) {
        val estadoMeta = estado.metas.firstOrNull { it.meta.id == detalle.id }
        ModalDetalleMeta(
            meta = detalle,
            estadoMeta = estadoMeta,
            cuentas = estado.cuentas,
            aportes = viewModel.aportesDetalle,
            error = viewModel.errorAporte,
            onRegistrar = { monto, cuentaId, fecha ->
                viewModel.registrarAporte(monto, cuentaId, fecha)
            },
            onCerrar = viewModel::cerrarDetalle
        )
    }

    metaAEliminar?.let { meta ->
        com.andrespar2.wallance.nucleo.diseno.componentes.DialogoConfirmacion(
            titulo = "¿Eliminar esta meta?",
            mensaje = "Se eliminarán la meta y su historial de aportes. Los movimientos registrados se conservan.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = {
                viewModel.eliminar(meta)
                metaAEliminar = null
            },
            onCancelar = { metaAEliminar = null }
        )
    }
}

@Composable
private fun TarjetaMeta(
    item: EstadoMeta,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val meta = item.meta
    val fraccion = (item.porcentaje.toFloat() / 100f).coerceIn(0f, 1f)
    TarjetaWallance(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meta.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = ColoresWallance.TextoPrincipal
                    )
                    meta.fechaObjetivo?.let { fecha ->
                        Text(
                            text = "Meta para ${Utilidades.formatearFechaCorta(fecha)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColoresWallance.TextoSecundario
                        )
                    }
                }
                Text(
                    text = "${item.porcentaje.setScale(0, java.math.RoundingMode.HALF_UP)}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresWallance.AmarilloPrincipal
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { fraccion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (item.porcentaje >= BigDecimal(100)) ColoresWallance.VerdeGanancia
                else ColoresWallance.AmarilloPrincipal,
                trackColor = ColoresWallance.SuperficieElevada
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Ahorrado ${Utilidades.formatearDinero(item.aportado, meta.moneda)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.VerdeGanancia
                )
                Text(
                    text = "de ${Utilidades.formatearDinero(meta.montoObjetivo, meta.moneda)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoPrincipal
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Falta ${Utilidades.formatearDinero(item.pendiente, meta.moneda)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.TextoSecundario
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
private fun ModalFormularioMeta(
    meta: MetaAhorro,
    cuentas: List<Cuenta>,
    moneda: String,
    error: String?,
    onGuardar: (String, String, String, Long?, Long?) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(meta.id) { mutableStateOf(meta.nombre) }
    var monto by remember(meta.id) {
        mutableStateOf(
            if (meta.id == 0L) ""
            else meta.montoObjetivo.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        )
    }
    var monedaSel by remember(meta.id) { mutableStateOf(meta.moneda) }
    var fecha by remember(meta.id) { mutableStateOf(meta.fechaObjetivo ?: System.currentTimeMillis()) }
    var cuentaSel by remember(meta.id) {
        mutableStateOf(cuentas.firstOrNull { it.id == meta.cuentaId })
    }

    ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (meta.id == 0L) "Nueva meta de ahorro" else "Editar meta"
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            CampoTexto(valor = nombre, onValorCambiado = { nombre = it }, etiqueta = "Nombre")
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = monto,
                onValorCambiado = { monto = it },
                etiqueta = "Monto objetivo",
                esDinero = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = monedaSel,
                opciones = com.andrespar2.wallance.nucleo.finanzas.MotorDivisas.MONEDAS.map { it.first },
                etiqueta = "Moneda",
                textoDe = { codigo ->
                    "$codigo · ${com.andrespar2.wallance.nucleo.finanzas.MotorDivisas.nombreDeMoneda(codigo)}"
                },
                onSeleccion = { monedaSel = it },
                textoValor = "$monedaSel · ${com.andrespar2.wallance.nucleo.finanzas.MotorDivisas.nombreDeMoneda(monedaSel)}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorFecha(valor = fecha, onSeleccion = { fecha = it }, etiqueta = "Fecha objetivo")
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = cuentaSel,
                opciones = listOf<Cuenta?>(null) + cuentas,
                etiqueta = "Cuenta sugerida",
                textoDe = { it?.nombre ?: "Sin cuenta asociada" },
                onSeleccion = { cuentaSel = it },
                textoValor = cuentaSel?.nombre ?: "Sin cuenta asociada"
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
                onClick = { onGuardar(nombre, monto, monedaSel, fecha, cuentaSel?.id) }
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

@Composable
private fun ModalDetalleMeta(
    meta: MetaAhorro,
    estadoMeta: EstadoMeta?,
    cuentas: List<Cuenta>,
    aportes: List<com.andrespar2.wallance.nucleo.basedatos.entidades.Aporte>,
    error: String?,
    onRegistrar: (String, Long?, Long) -> Unit,
    onCerrar: () -> Unit
) {
    var monto by remember(meta.id) { mutableStateOf("") }
    var cuentaSel by remember(meta.id) { mutableStateOf<Cuenta?>(null) }
    var fecha by remember(meta.id) { mutableStateOf(System.currentTimeMillis()) }

    ModalBottomSheetFlujo(onDismissRequest = onCerrar, titulo = meta.nombre) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val aportado = estadoMeta?.aportado ?: BigDecimal.ZERO
            val pendiente = estadoMeta?.pendiente ?: meta.montoObjetivo
            Text(
                text = "Ahorrado ${Utilidades.formatearDinero(aportado, meta.moneda)} de " +
                    Utilidades.formatearDinero(meta.montoObjetivo, meta.moneda),
                style = MaterialTheme.typography.bodyMedium,
                color = ColoresWallance.TextoPrincipal
            )
            Text(
                text = "Te falta ${Utilidades.formatearDinero(pendiente, meta.moneda)}",
                style = MaterialTheme.typography.labelMedium,
                color = ColoresWallance.TextoSecundario
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Registrar aporte",
                style = MaterialTheme.typography.labelLarge,
                color = ColoresWallance.AmarilloPrincipal
            )
            Spacer(modifier = Modifier.height(8.dp))
            CampoTexto(
                valor = monto,
                onValorCambiado = { monto = it },
                etiqueta = "Monto",
                esDinero = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            SelectorPorHoja(
                valor = cuentaSel,
                opciones = listOf<Cuenta?>(null) + cuentas,
                etiqueta = "Cuenta de origen",
                textoDe = { it?.nombre ?: "Solo seguimiento (sin descontar)" },
                onSeleccion = { cuentaSel = it },
                textoValor = cuentaSel?.nombre ?: "Solo seguimiento (sin descontar)"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SelectorFecha(valor = fecha, onSeleccion = { fecha = it }, etiqueta = "Fecha")
            if (cuentaSel == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sin cuenta, el aporte solo actualiza el progreso de la meta.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoSecundario
                )
            }
            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, color = ColoresWallance.RojoPerdida, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            BotonPrincipal(
                texto = "Aportar",
                onClick = {
                    onRegistrar(monto, cuentaSel?.id, fecha)
                    monto = ""
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Historial de aportes",
                style = MaterialTheme.typography.labelLarge,
                color = ColoresWallance.AmarilloPrincipal
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (aportes.isEmpty()) {
                Text(
                    text = "Todavía no hay aportes registrados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.TextoSecundario
                )
            } else {
                Column(modifier = Modifier.heightIn(max = 220.dp)) {
                    LazyColumn {
                        items(aportes, key = { it.id }) { aporte ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Utilidades.formatearDinero(aporte.monto, meta.moneda),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ColoresWallance.VerdeGanancia
                                    )
                                    Text(
                                        text = Utilidades.formatearFechaCorta(aporte.fecha),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ColoresWallance.TextoSecundario
                                    )
                                }
                            }
                            Separator()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BotonPrincipal(
                texto = "Cerrar",
                colorFondo = ColoresWallance.Tarjeta,
                colorTexto = ColoresWallance.TextoPrincipal,
                onClick = onCerrar
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
