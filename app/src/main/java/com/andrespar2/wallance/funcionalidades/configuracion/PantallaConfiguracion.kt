package com.andrespar2.wallance.funcionalidades.configuracion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.Separator
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas

/**
 * Configuración general: acceso a las secciones de gestión y preferencias
 * de la aplicación.
 */
@Composable
fun PantallaConfiguracion(
    onIrA: (String) -> Unit,
    monedaPrincipal: String,
    viewModel: ConfiguracionViewModel = hiltViewModel()
) {
    val ocultarMontos by viewModel.ocultarMontos.collectAsStateWithLifecycle()
    val notificaciones by viewModel.notificacionesHabilitadas.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
        )
    ) {
        item {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.titleLarge,
                color = ColoresWallance.TextoPrincipal
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "Gestión",
                style = MaterialTheme.typography.labelLarge,
                color = ColoresWallance.AmarilloPrincipal
            )
            Spacer(modifier = Modifier.height(8.dp))
            TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                Column {
                    FilaSeccion("Cuentas", Icons.Default.AccountBalanceWallet) {
                        onIrA(com.andrespar2.wallance.nucleo.navegacion.Rutas.CUENTAS)
                    }
                    Separator()
                    FilaSeccion("Categorías", Icons.Default.Category) {
                        onIrA(com.andrespar2.wallance.nucleo.navegacion.Rutas.CATEGORIAS)
                    }
                    Separator()
                    FilaSeccion("Presupuestos", Icons.Default.TrackChanges) {
                        onIrA(com.andrespar2.wallance.nucleo.navegacion.Rutas.PRESUPUESTOS)
                    }
                    Separator()
                    FilaSeccion("Metas de ahorro", Icons.Default.Savings) {
                        onIrA(com.andrespar2.wallance.nucleo.navegacion.Rutas.AHORROS)
                    }
                    Separator()
                    FilaSeccion("Divisas", Icons.Default.AttachMoney) {
                        onIrA(com.andrespar2.wallance.nucleo.navegacion.Rutas.DIVISAS)
                    }
                    Separator()
                    FilaSeccion("Suscripciones", Icons.Default.Repeat) {
                        onIrA(com.andrespar2.wallance.nucleo.navegacion.Rutas.SUSCRIPCIONES)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "Preferencias",
                style = MaterialTheme.typography.labelLarge,
                color = ColoresWallance.AmarilloPrincipal
            )
            Spacer(modifier = Modifier.height(8.dp))
            TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                Column {
                    FilaInterruptor(
                        texto = "Ocultar montos",
                        detalle = "Enmascara los saldos al abrir la aplicación",
                        icono = Icons.Default.VisibilityOff,
                        activo = ocultarMontos,
                        onCambio = viewModel::fijarOcultarMontos
                    )
                    Separator()
                    FilaInterruptor(
                        texto = "Notificaciones",
                        detalle = "Avisos de vencimientos y presupuestos",
                        icono = Icons.Default.Notifications,
                        activo = notificaciones,
                        onCambio = viewModel::fijarNotificaciones
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Moneda principal",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja(
                        valor = monedaPrincipal,
                        opciones = MotorDivisas.MONEDAS.map { it.first },
                        etiqueta = "Moneda",
                        textoDe = { codigo ->
                            "$codigo · ${MotorDivisas.nombreDeMoneda(codigo)}"
                        },
                        onSeleccion = viewModel::fijarMonedaPrincipal,
                        textoValor = "$monedaPrincipal · ${MotorDivisas.nombreDeMoneda(monedaPrincipal)}"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Wallance",
                        style = MaterialTheme.typography.titleMedium,
                        color = ColoresWallance.TextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tu dinero. Tu equilibrio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Versión 1.0.0 · andrespar2",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaSeccion(
    texto: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = ColoresWallance.AmarilloPrincipal,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresWallance.TextoPrincipal,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ColoresWallance.TextoSecundario
        )
    }
}

@Composable
private fun FilaInterruptor(
    texto: String,
    detalle: String,
    icono: ImageVector,
    activo: Boolean,
    onCambio: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCambio(!activo) }
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = ColoresWallance.TextoSecundario,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresWallance.TextoPrincipal
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.labelMedium,
                color = ColoresWallance.TextoSecundario
            )
        }
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ColoresWallance.AmarilloPrincipal,
                checkedThumbColor = ColoresWallance.NegroAmoled,
                uncheckedTrackColor = ColoresWallance.Tarjeta,
                uncheckedThumbColor = ColoresWallance.TextoSecundario,
                uncheckedBorderColor = ColoresWallance.BordeSutil
            )
        )
    }
}
