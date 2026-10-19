package com.andrespar2.wallance.funcionalidades.cuentas

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCuenta
import com.andrespar2.wallance.nucleo.basedatos.repositorios.CuentaConSaldo
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas
import java.math.BigDecimal

/**
 * Pantalla para listar y gestionar cuentas financieras.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCuentas(
    onVolver: () -> Unit,
    viewModel: CuentasViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarEliminar by remember { mutableStateOf<CuentaConSaldo?>(null) }
    val cuentaEnEdicion = viewModel.cuentaEnEdicion
    val errorFormulario = viewModel.errorFormulario

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Cuentas", color = ColoresWallance.TextoPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = ColoresWallance.TextoPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColoresWallance.NegroSecundario,
                    titleContentColor = ColoresWallance.TextoPrincipal,
                    navigationIconContentColor = ColoresWallance.TextoPrincipal
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::nuevaCuenta,
                containerColor = ColoresWallance.AmarilloPrincipal,
                contentColor = ColoresWallance.NegroAmoled
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva cuenta")
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
            } else if (estado.cuentas.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin cuentas",
                    mensaje = "Agrega una cuenta para realizar un seguimiento de tus saldos."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(estado.cuentas, key = { it.cuenta.id }) { cuentaConSaldo ->
                        FilaCuenta(
                            cuentaConSaldo = cuentaConSaldo,
                            onEditar = { viewModel.editarCuenta(cuentaConSaldo.cuenta) },
                            onArchivar = { mostrarEliminar = cuentaConSaldo }
                        )
                    }
                }
            }
        }
    }

    if (mostrarEliminar != null) {
        val cuentaConSaldo = mostrarEliminar ?: return
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarEliminar = null },
            containerColor = ColoresWallance.SuperficieElevada,
            titleContentColor = ColoresWallance.TextoPrincipal,
            textContentColor = ColoresWallance.TextoSecundario,
            title = { Text("¿Archivar esta cuenta?") },
            text = {
                Text(
                    "Los movimientos asociados se conservarán. Puedes volver a activarla más adelante."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.archivarCuenta(cuentaConSaldo.cuenta)
                        mostrarEliminar = null
                    }
                ) {
                    Text("Archivar", color = ColoresWallance.RojoPerdida)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { mostrarEliminar = null }) {
                    Text("Cancelar", color = ColoresWallance.TextoSecundario)
                }
            }
        )
    }

    cuentaEnEdicion?.let { cuenta ->
        ModalFormularioCuenta(
            cuenta = cuenta,
            error = errorFormulario,
            onGuardar = { nombre, tipo, moneda, saldo ->
                viewModel.guardarCuenta(nombre, tipo, moneda, saldo)
            },
            onCancelar = viewModel::cerrarFormulario
        )
    }
}
@Composable
private fun FilaCuenta(
    cuentaConSaldo: CuentaConSaldo,
    onEditar: () -> Unit,
    onArchivar: () -> Unit
) {
    val cuenta = cuentaConSaldo.cuenta
    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = cuenta.nombre, style = MaterialTheme.typography.titleMedium, color = ColoresWallance.TextoPrincipal)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${cuenta.tipo.etiqueta} · ${cuenta.moneda}", style = MaterialTheme.typography.labelMedium, color = ColoresWallance.TextoSecundario)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = Utilidades.formatearDinero(cuentaConSaldo.saldo, cuenta.moneda), style = MaterialTheme.typography.titleMedium, color = ColoresWallance.TextoPrincipal)
            }
            Row {
                Text(text = "Editar", style = MaterialTheme.typography.labelMedium, color = ColoresWallance.AmarilloPrincipal, modifier = Modifier.clickable(onClick = onEditar).padding(6.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Archivar", style = MaterialTheme.typography.labelMedium, color = ColoresWallance.RojoPerdida, modifier = Modifier.clickable(onClick = onArchivar).padding(6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalFormularioCuenta(
    cuenta: com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta,
    error: String?,
    onGuardar: (String, TipoCuenta, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(cuenta.id) { mutableStateOf(cuenta.nombre) }
    var tipo by remember(cuenta.id) { mutableStateOf(cuenta.tipo) }
    var moneda by remember(cuenta.id) { mutableStateOf(cuenta.moneda) }
    var saldo by remember(cuenta.id) {
        mutableStateOf(
            if (cuenta.id == 0L) {
                cuenta.saldoInicial.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } else {
                BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            }
        )
    }

    com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (cuenta.id == 0L) "Nueva cuenta" else "Editar cuenta"
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            CampoTexto(valor = nombre, onValorCambiado = { nombre = it }, etiqueta = "Nombre")
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = tipo,
                opciones = TipoCuenta.values().toList(),
                etiqueta = "Tipo",
                textoDe = { it.etiqueta },
                onSeleccion = { tipo = it },
                textoValor = tipo.etiqueta
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = moneda,
                opciones = MotorDivisas.MONEDAS.map { it.first },
                etiqueta = "Moneda",
                textoDe = { codigo -> "$codigo · ${MotorDivisas.nombreDeMoneda(codigo)}" },
                onSeleccion = { moneda = it },
                textoValor = "$moneda · ${MotorDivisas.nombreDeMoneda(moneda)}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(valor = saldo, onValorCambiado = { saldo = it }, etiqueta = "Saldo inicial", esDinero = true)
            if (cuenta.id == 0L) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El saldo inicial es opcional. Se puede ajustar más tarde.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoSecundario
                )
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = ColoresWallance.RojoPerdida, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            BotonPrincipal(texto = "Guardar", onClick = { onGuardar(nombre, tipo, moneda, saldo) })
            Spacer(modifier = Modifier.height(8.dp))
            BotonPrincipal(texto = "Cancelar", colorFondo = ColoresWallance.Tarjeta, colorTexto = ColoresWallance.TextoPrincipal, onClick = onCancelar)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
