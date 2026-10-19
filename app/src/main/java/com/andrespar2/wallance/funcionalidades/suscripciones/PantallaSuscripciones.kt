package com.andrespar2.wallance.funcionalidades.suscripciones

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
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoSuscripcion
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.Suscripcion
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
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas
import java.math.BigDecimal

/**
 * Suscripciones y gastos recurrentes: recordatorios de cobros periódicos.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PantallaSuscripciones(
    onVolver: () -> Unit,
    viewModel: SuscripcionesViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var suscripcionAEliminar by remember { mutableStateOf<Suscripcion?>(null) }

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Suscripciones", color = ColoresWallance.TextoPrincipal) },
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
                onClick = viewModel::nuevaSuscripcion,
                containerColor = ColoresWallance.AmarilloPrincipal,
                contentColor = ColoresWallance.NegroAmoled
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva suscripción")
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "Costo mensual estimado",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColoresWallance.TextoSecundario
                                )
                                Text(
                                    text = Utilidades.formatearDinero(estado.totalMensual, estado.moneda),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ColoresWallance.AmarilloPrincipal
                                )
                                Text(
                                    text = "Suma de las suscripciones activas normalizada a un mes.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColoresWallance.TextoSecundario
                                )
                            }
                        }
                    }

                    if (estado.suscripciones.isEmpty()) {
                        item {
                            EstadoVacio(
                                titulo = "Sin suscripciones",
                                mensaje = "Registra tus cobros recurrentes para anticipar tus gastos.",
                                icono = Icons.Default.Repeat
                            )
                        }
                    } else {
                        items(estado.suscripciones, key = { it.id }) { suscripcion ->
                            TarjetaSuscripcion(
                                suscripcion = suscripcion,
                                estado = estado,
                                onEditar = { viewModel.editarSuscripcion(suscripcion) },
                                onAlternar = {
                                    val nuevo = if (suscripcion.estado == EstadoSuscripcion.ACTIVA) {
                                        EstadoSuscripcion.PAUSADA
                                    } else {
                                        EstadoSuscripcion.ACTIVA
                                    }
                                    viewModel.cambiarEstado(suscripcion, nuevo)
                                },
                                onEliminar = { suscripcionAEliminar = suscripcion }
                            )
                        }
                    }
                }
            }
        }
    }

    val enEdicion = viewModel.suscripcionEnEdicion
    if (enEdicion != null) {
        ModalFormularioSuscripcion(
            suscripcion = enEdicion,
            cuentas = estado.cuentas,
            categorias = estado.categorias,
            moneda = estado.moneda,
            error = viewModel.errorFormulario,
            onGuardar = { nombre, costo, moneda, frecuencia, proximo, cuentaId, categoriaId, est ->
                viewModel.guardar(nombre, costo, moneda, frecuencia, proximo, cuentaId, categoriaId, est)
            },
            onCancelar = viewModel::cerrarFormulario
        )
    }

    suscripcionAEliminar?.let { suscripcion ->
        com.andrespar2.wallance.nucleo.diseno.componentes.DialogoConfirmacion(
            titulo = "¿Eliminar esta suscripción?",
            mensaje = "Se quitará el recordatorio. Los movimientos ya registrados no se modifican.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = {
                viewModel.eliminar(suscripcion)
                suscripcionAEliminar = null
            },
            onCancelar = { suscripcionAEliminar = null }
        )
    }
}

@Composable
private fun TarjetaSuscripcion(
    suscripcion: Suscripcion,
    estado: EstadoSuscripciones,
    onEditar: () -> Unit,
    onAlternar: () -> Unit,
    onEliminar: () -> Unit
) {
    val colorEstado = when (suscripcion.estado) {
        EstadoSuscripcion.ACTIVA -> ColoresWallance.VerdeGanancia
        EstadoSuscripcion.PAUSADA -> ColoresWallance.AmarilloPrincipal
        EstadoSuscripcion.CANCELADA -> ColoresWallance.RojoPerdida
    }
    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colorEstado)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suscripcion.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = ColoresWallance.TextoPrincipal
                    )
                    Text(
                        text = "${suscripcion.frecuencia.etiqueta} · próximo " +
                            Utilidades.formatearFechaCorta(suscripcion.proximoCobro),
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                }
                Text(
                    text = Utilidades.formatearDinero(suscripcion.costo, suscripcion.moneda),
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresWallance.TextoPrincipal
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = suscripcion.estado.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorEstado
                )
                Row {
                    Text(
                        text = if (suscripcion.estado == EstadoSuscripcion.ACTIVA) "Pausar" else "Activar",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.AmarilloPrincipal,
                        modifier = Modifier.clickable(onClick = onAlternar).padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
private fun ModalFormularioSuscripcion(
    suscripcion: Suscripcion,
    cuentas: List<Cuenta>,
    categorias: List<Categoria>,
    moneda: String,
    error: String?,
    onGuardar: (
        String, String, String, FrecuenciaCuota, Long, Long?, Long?, EstadoSuscripcion
    ) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(suscripcion.id) { mutableStateOf(suscripcion.nombre) }
    var costo by remember(suscripcion.id) {
        mutableStateOf(
            if (suscripcion.id == 0L) ""
            else suscripcion.costo.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        )
    }
    var monedaSel by remember(suscripcion.id) { mutableStateOf(suscripcion.moneda) }
    var frecuencia by remember(suscripcion.id) { mutableStateOf(suscripcion.frecuencia) }
    var proximo by remember(suscripcion.id) { mutableStateOf(suscripcion.proximoCobro) }
    var cuentaSel by remember(suscripcion.id) {
        mutableStateOf(cuentas.firstOrNull { it.id == suscripcion.cuentaId })
    }
    var categoriaSel by remember(suscripcion.id) {
        mutableStateOf(categorias.firstOrNull { it.id == suscripcion.categoriaId })
    }
    var estadoSel by remember(suscripcion.id) { mutableStateOf(suscripcion.estado) }

    ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (suscripcion.id == 0L) "Nueva suscripción" else "Editar suscripción"
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            CampoTexto(valor = nombre, onValorCambiado = { nombre = it }, etiqueta = "Nombre")
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = costo,
                onValorCambiado = { costo = it },
                etiqueta = "Costo",
                esDinero = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = monedaSel,
                opciones = MotorDivisas.MONEDAS.map { it.first },
                etiqueta = "Moneda",
                textoDe = { "$it · ${MotorDivisas.nombreDeMoneda(it)}" },
                onSeleccion = { monedaSel = it },
                textoValor = "$monedaSel · ${MotorDivisas.nombreDeMoneda(monedaSel)}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = frecuencia,
                opciones = FrecuenciaCuota.values().toList(),
                etiqueta = "Frecuencia",
                textoDe = { it.etiqueta },
                onSeleccion = { frecuencia = it },
                textoValor = frecuencia.etiqueta
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorFecha(valor = proximo, onSeleccion = { proximo = it }, etiqueta = "Próximo cobro")
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = cuentaSel,
                opciones = listOf<Cuenta?>(null) + cuentas,
                etiqueta = "Cuenta",
                textoDe = { it?.nombre ?: "Sin cuenta" },
                onSeleccion = { cuentaSel = it },
                textoValor = cuentaSel?.nombre ?: "Sin cuenta"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = categoriaSel,
                opciones = listOf<Categoria?>(null) + categorias,
                etiqueta = "Categoría",
                textoDe = { it?.nombre ?: "Sin categoría" },
                onSeleccion = { categoriaSel = it },
                textoValor = categoriaSel?.nombre ?: "Sin categoría"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = estadoSel,
                opciones = EstadoSuscripcion.values().toList(),
                etiqueta = "Estado",
                textoDe = { it.etiqueta },
                onSeleccion = { estadoSel = it },
                textoValor = estadoSel.etiqueta
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
                        costo,
                        monedaSel,
                        frecuencia,
                        proximo,
                        cuentaSel?.id,
                        categoriaSel?.id,
                        estadoSel
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
