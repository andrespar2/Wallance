package com.andrespar2.wallance.funcionalidades.inicio

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.comun.RangoPeriodo
import com.andrespar2.wallance.nucleo.basedatos.entidades.esIngreso
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import com.andrespar2.wallance.nucleo.publicidad.BannerPublicidad

/**
 * Panel principal de finanzas de Wallance.
 * Muestra el estado financiero real del usuario, sin datos ficticios.
 */
@Composable
fun PantallaInicio(
    moneda: String,
    ocultarMontos: Boolean,
    onRegistrarMovimiento: () -> Unit,
    onVerMovimientos: () -> Unit,
    onVerPrestamos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InicioViewModel = hiltViewModel()
) {
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ColoresWallance.NegroAmoled),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Saldo total.
        item {
            val saldo = resumen?.saldoTotal
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Saldo total",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.TextoSecundario
                )
                Text(
                    text = if (ocultarMontos) "••••••"
                    else if (saldo != null) Utilidades.formatearDinero(saldo, moneda) else "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = ColoresWallance.TextoPrincipal
                )
            }
        }

        // Filtros de periodo.
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RangoPeriodo.entries.filter { it != RangoPeriodo.PERSONALIZADO }.forEach { periodo ->
                    val activo = viewModel.periodoSeleccionado == periodo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (activo) ColoresWallance.AmarilloPrincipal
                                else ColoresWallance.Tarjeta
                            )
                            .clickable { viewModel.cambiarPeriodo(periodo) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = periodo.etiqueta,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (activo) ColoresWallance.NegroAmoled
                            else ColoresWallance.TextoSecundario
                        )
                    }
                }
            }
        }

        // Ingresos, gastos y balance del periodo.
        item {
            val r = resumen
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaDato(
                    titulo = "Ingresos",
                    valor = if (ocultarMontos) "••••" else formatear(r?.ingresos, moneda),
                    color = ColoresWallance.VerdeGanancia,
                    icono = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                TarjetaDato(
                    titulo = "Gastos",
                    valor = if (ocultarMontos) "••••" else formatear(r?.gastos, moneda),
                    color = ColoresWallance.RojoPerdida,
                    icono = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val balance = resumen?.balance
            TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Balance del periodo",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColoresWallance.TextoSecundario
                        )
                        Text(
                            text = if (ocultarMontos) "••••" else formatear(balance, moneda),
                            style = MaterialTheme.typography.headlineMedium,
                            color = when {
                                balance == null -> ColoresWallance.TextoPrincipal
                                balance.signum() > 0 -> ColoresWallance.VerdeGanancia
                                balance.signum() < 0 -> ColoresWallance.RojoPerdida
                                else -> ColoresWallance.TextoPrincipal
                            }
                        )
                    }
                    Text(
                        text = viewModel.intervaloActual.etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        color = ColoresWallance.TextoSecundario
                    )
                }
            }
        }

        // Préstamos: dinero prestado, deuda y ganancias.
        item {
            val r = resumen
            TarjetaWallance(modifier = Modifier.fillMaxWidth().clickable { onVerPrestamos() }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = ColoresWallance.AmarilloPrincipal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Préstamos",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColoresWallance.TextoPrincipal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilaResumen("Por cobrar", if (ocultarMontos) "••••" else formatear(r?.dineroPrestado, moneda))
                    FilaResumen("Que debo", if (ocultarMontos) "••••" else formatear(r?.dineroQueDebo, moneda))
                    FilaResumen(
                        "Ganancia esperada",
                        if (ocultarMontos) "••••" else formatear(r?.gananciaEsperada, moneda),
                        ColoresWallance.VerdeGanancia
                    )
                    FilaResumen(
                        "Ganancia realizada",
                        if (ocultarMontos) "••••" else formatear(r?.gananciaRealizada, moneda),
                        ColoresWallance.VerdeGanancia
                    )
                }
            }
        }

        // Próximos vencimientos.
        val vencimientos = resumen?.proximosVencimientos.orEmpty()
        if (vencimientos.isNotEmpty()) {
            item {
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = ColoresWallance.RojoPerdida,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Próximos vencimientos",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColoresWallance.TextoPrincipal
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        vencimientos.forEach { vencimiento ->
                            val dias = Utilidades.diasEntre(
                                System.currentTimeMillis(),
                                vencimiento.cuota.fechaVencimiento
                            )
                            FilaResumen(
                                etiqueta = "Cuota ${vencimiento.cuota.numero} — " +
                                    (vencimiento.contacto?.nombreCompleto ?: "Contacto"),
                                valor = buildString {
                                    if (!ocultarMontos) {
                                        append(Utilidades.formatearDinero(vencimiento.cuota.valorTotal, moneda))
                                        append(" · ")
                                    }
                                    append(
                                        if (dias < 0) "vencida hace ${-dias} día(s)"
                                        else "en $dias día(s)"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Presupuestos.
        val presupuestos = resumen?.presupuestos.orEmpty()
        if (presupuestos.isNotEmpty()) {
            item {
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Presupuestos",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColoresWallance.TextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        presupuestos.forEach { estado ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = estado.presupuesto.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColoresWallance.TextoPrincipal
                                    )
                                    Text(
                                        text = if (ocultarMontos) "••••"
                                        else "${estado.porcentajeConsumido} %",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (estado.porcentajeConsumido >= java.math.BigDecimal(100))
                                            ColoresWallance.RojoPerdida
                                        else ColoresWallance.TextoSecundario
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        estado.porcentajeConsumido
                                            .divide(BigDecimalCien, java.math.RoundingMode.HALF_UP)
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (estado.porcentajeConsumido >= BigDecimalCien)
                                        ColoresWallance.RojoPerdida else ColoresWallance.AmarilloPrincipal,
                                    trackColor = ColoresWallance.SuperficieElevada
                                )
                            }
                        }
                    }
                }
            }
        }

        // Metas de ahorro.
        val metas = resumen?.metas.orEmpty()
        if (metas.isNotEmpty()) {
            item {
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Metas de ahorro",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColoresWallance.TextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        metas.forEach { estado ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = estado.meta.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColoresWallance.TextoPrincipal
                                    )
                                    Text(
                                        text = if (ocultarMontos) "••••" else "${estado.porcentaje} %",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColoresWallance.VerdeGanancia
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        estado.porcentaje
                                            .divide(BigDecimalCien, java.math.RoundingMode.HALF_UP)
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ColoresWallance.VerdeGanancia,
                                    trackColor = ColoresWallance.SuperficieElevada
                                )
                            }
                        }
                    }
                }
            }
        }

        // Últimos movimientos.
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresWallance.TextoPrincipal
                )
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColoresWallance.AmarilloPrincipal,
                    modifier = Modifier.clickable { onVerMovimientos() }
                )
            }
        }

        val movimientos = resumen?.ultimosMovimientos.orEmpty()
        if (movimientos.isEmpty()) {
            item {
                EstadoVacio(
                    titulo = "Sin movimientos",
                    mensaje = "Registra tu primer ingreso o gasto con el botón inferior.",
                    icono = Icons.Default.Receipt
                )
            }
        } else {
            items(movimientos.size) { indice ->
                val movimiento = movimientos[indice]
                val esIngreso = movimiento.tipo.esIngreso
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (esIngreso) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (esIngreso) ColoresWallance.VerdeGanancia else ColoresWallance.RojoPerdida
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movimiento.descripcion.ifBlank { movimiento.tipo.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                                style = MaterialTheme.typography.bodyLarge,
                                color = ColoresWallance.TextoPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Utilidades.formatearFechaCorta(movimiento.fecha),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColoresWallance.TextoSecundario
                            )
                        }
                        Text(
                            text = if (ocultarMontos) "••••"
                            else (if (esIngreso) "+" else "−") +
                                Utilidades.formatearDinero(movimiento.monto, movimiento.moneda),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (esIngreso) ColoresWallance.VerdeGanancia else ColoresWallance.RojoPerdida
                        )
                    }
                }
            }
        }

        item {
            BannerPublicidad()
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

private val BigDecimalCien = java.math.BigDecimal(100)

private fun formatear(cantidad: java.math.BigDecimal?, moneda: String): String =
    if (cantidad == null) "—" else Utilidades.formatearDinero(cantidad, moneda)

@Composable
private fun TarjetaDato(
    titulo: String,
    valor: String,
    color: Color,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    TarjetaWallance(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = ColoresWallance.TextoSecundario)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(valor, style = MaterialTheme.typography.titleMedium, color = ColoresWallance.TextoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilaResumen(
    etiqueta: String,
    valor: String,
    colorValor: Color = ColoresWallance.TextoPrincipal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = ColoresWallance.TextoSecundario)
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = colorValor, maxLines = 1)
    }
}
