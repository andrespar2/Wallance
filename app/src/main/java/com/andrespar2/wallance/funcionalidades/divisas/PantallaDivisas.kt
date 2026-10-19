package com.andrespar2.wallance.funcionalidades.divisas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas
import java.math.RoundingMode

/**
 * Cotizaciones de divisas y convertidor. Usa datos reales de la API pública;
 * sin conexión se muestran las últimas tasas guardadas con su fecha.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PantallaDivisas(
    onVolver: () -> Unit,
    viewModel: DivisasViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Divisas", color = ColoresWallance.TextoPrincipal) },
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
                    IconButton(onClick = { viewModel.actualizar() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar cotizaciones",
                            tint = ColoresWallance.AmarilloPrincipal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColoresWallance.NegroSecundario,
                    titleContentColor = ColoresWallance.TextoPrincipal
                )
            )
        }
    ) { relleno ->
        if (estado.cargando) {
            Cargando(modifier = Modifier.fillMaxSize().padding(relleno))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Moneda principal",
                                style = MaterialTheme.typography.labelMedium,
                                color = ColoresWallance.TextoSecundario
                            )
                            Text(
                                text = "${estado.monedaPrincipal} · " +
                                    MotorDivisas.nombreDeMoneda(estado.monedaPrincipal),
                                style = MaterialTheme.typography.titleMedium,
                                color = ColoresWallance.TextoPrincipal
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (estado.fechaActualizacion > 0) {
                                    "Última actualización: " +
                                        Utilidades.formatearFechaHora(estado.fechaActualizacion) +
                                        if (estado.fuente.isNotBlank()) " · ${estado.fuente}" else ""
                                } else {
                                    "Aún no hay cotizaciones guardadas."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = ColoresWallance.TextoSecundario
                            )
                            if (estado.actualizando) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Actualizando…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColoresWallance.AmarilloPrincipal
                                )
                            }
                            estado.mensaje?.let { aviso ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = aviso,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColoresWallance.TextoSecundario
                                )
                            }
                        }
                    }
                }

                item { ConvertidorDivisas(estado.monedaPrincipal, viewModel) }

                item {
                    Text(
                        text = "Parejas principales",
                        style = MaterialTheme.typography.labelLarge,
                        color = ColoresWallance.AmarilloPrincipal
                    )
                }

                if (estado.parejas.isEmpty()) {
                    item {
                        TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No hay cotizaciones disponibles. Toca el botón de " +
                                    "actualizar para intentar descargarlas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColoresWallance.TextoSecundario
                            )
                        }
                    }
                } else {
                    items(estado.parejas, key = { "${it.base}-${it.destino}" }) { fila ->
                        TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${fila.base}/${fila.destino}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ColoresWallance.TextoPrincipal
                                    )
                                    Text(
                                        text = "1 ${fila.base} = " +
                                            fila.tasa.setScale(4, RoundingMode.HALF_UP).toPlainString() +
                                            " ${fila.destino}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColoresWallance.TextoSecundario
                                    )
                                }
                                Text(
                                    text = Utilidades.formatearDinero(fila.tasa, fila.destino),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ColoresWallance.AmarilloPrincipal
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Las cotizaciones provienen de ExchangeRate-API y se " +
                            "actualizan una vez al día. Wallance no inventa tasas: sin " +
                            "conexión se muestran los últimos valores guardados.",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConvertidorDivisas(
    monedaPrincipal: String,
    viewModel: DivisasViewModel
) {
    val codigos = MotorDivisas.MONEDAS.map { it.first }
    var monto by remember { mutableStateOf("1") }
    var origen by remember(monedaPrincipal) { mutableStateOf(monedaPrincipal) }
    var destino by remember { mutableStateOf(if (monedaPrincipal == "USD") "EUR" else "USD") }
    val conversion = viewModel.conversion
    val exitosa = viewModel.conversionExitosa

    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Convertidor",
                style = MaterialTheme.typography.titleMedium,
                color = ColoresWallance.TextoPrincipal
            )
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = monto,
                onValorCambiado = {
                    monto = it
                    viewModel.limpiarConversion()
                },
                etiqueta = "Cantidad",
                esDinero = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = origen,
                opciones = codigos,
                etiqueta = "De",
                textoDe = { "$it · ${MotorDivisas.nombreDeMoneda(it)}" },
                onSeleccion = {
                    origen = it
                    viewModel.limpiarConversion()
                },
                textoValor = "$origen · ${MotorDivisas.nombreDeMoneda(origen)}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectorPorHoja(
                valor = destino,
                opciones = codigos,
                etiqueta = "A",
                textoDe = { "$it · ${MotorDivisas.nombreDeMoneda(it)}" },
                onSeleccion = {
                    destino = it
                    viewModel.limpiarConversion()
                },
                textoValor = "$destino · ${MotorDivisas.nombreDeMoneda(destino)}"
            )
            conversion?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (exitosa) ColoresWallance.VerdeGanancia
                    else ColoresWallance.RojoPerdida
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            BotonPrincipal(
                texto = "Convertir",
                onClick = { viewModel.convertir(monto, origen, destino) }
            )
        }
    }
}
