package com.andrespar2.wallance.funcionalidades.estadisticas

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.Cargando
import com.andrespar2.wallance.nucleo.diseno.componentes.EstadoVacio
import com.andrespar2.wallance.nucleo.diseno.componentes.FilaDato
import com.andrespar2.wallance.nucleo.diseno.componentes.Separator
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import java.math.BigDecimal

/**
 * Pantalla de estadísticas con gráficos dibujados en Canvas propio
 * (barras de evolución y anillo de categorías), sin librerías externas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEstadisticas(
    onVolver: () -> Unit,
    viewModel: EstadisticasViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val periodo by viewModel.periodoActual.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ColoresWallance.NegroAmoled,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", color = ColoresWallance.TextoPrincipal) },
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
        }
    ) { relleno ->
        if (estado.cargando) {
            Cargando(modifier = Modifier.fillMaxSize().padding(relleno))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    FilaPeriodos(periodo = periodo, onCambio = viewModel::cambiarPeriodo)
                }
                item {
                    TarjetaResumen(estado = estado)
                }
                item {
                    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Ingresos vs gastos (6 meses)",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColoresWallance.TextoPrincipal
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            GraficoBarras(puntos = estado.evolucion)
                        }
                    }
                }
                item {
                    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Gastos por categoría",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColoresWallance.TextoPrincipal
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            if (estado.categorias.isEmpty()) {
                                Text(
                                    text = "Sin gastos en el periodo seleccionado.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ColoresWallance.TextoSecundario
                                )
                            } else {
                                GraficoAnillo(categorias = estado.categorias)
                                Spacer(modifier = Modifier.height(12.dp))
                                LeyendaCategorias(categorias = estado.categorias, moneda = estado.moneda)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPeriodos(periodo: PeriodoEstadistica, onCambio: (PeriodoEstadistica) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodoEstadistica.entries.forEach { opcion ->
            FilterChip(
                selected = periodo == opcion,
                onClick = { onCambio(opcion) },
                label = { Text(opcion.etiqueta) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ColoresWallance.AmarilloPrincipal,
                    selectedLabelColor = ColoresWallance.NegroAmoled,
                    containerColor = ColoresWallance.Tarjeta,
                    labelColor = ColoresWallance.TextoSecundario
                )
            )
        }
    }
}

@Composable
private fun TarjetaResumen(estado: EstadoEstadisticas) {
    TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Balance del periodo",
                style = MaterialTheme.typography.titleMedium,
                color = ColoresWallance.TextoPrincipal
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilaDato(
                etiqueta = "Ingresos",
                valor = Utilidades.formatearDinero(estado.ingresos, estado.moneda),
                colorValor = ColoresWallance.VerdeGanancia
            )
            FilaDato(
                etiqueta = "Gastos",
                valor = Utilidades.formatearDinero(estado.gastos, estado.moneda),
                colorValor = ColoresWallance.RojoPerdida
            )
            Separator()
            FilaDato(
                etiqueta = "Balance",
                valor = Utilidades.formatearDinero(estado.balance, estado.moneda),
                colorValor = if (estado.balance.signum() >= 0) {
                    ColoresWallance.VerdeGanancia
                } else {
                    ColoresWallance.RojoPerdida
                },
                resaltado = true
            )
        }
    }
}

/** Gráfico de barras doble (ingresos y gastos) dibujado en Canvas. */
@Composable
private fun GraficoBarras(puntos: List<PuntoEvolucion>) {
    val colorIngreso = ColoresWallance.VerdeGanancia
    val colorGasto = ColoresWallance.RojoPerdida
    val colorTexto = ColoresWallance.TextoSecundario

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (puntos.isEmpty()) return@Canvas
        val maximo = puntos.maxOf { maxOf(it.ingresos, it.gastos) }
        if (maximo.signum() <= 0) return@Canvas

        val alturaUtil = size.height - 22f
        val anchoGrupo = size.width / puntos.size
        val anchoBarra = anchoGrupo * 0.26f
        val separacion = anchoGrupo * 0.06f

        puntos.forEachIndexed { indice, punto ->
            val baseX = anchoGrupo * indice
            val alturaIngreso = (punto.ingresos.toFloat() / maximo.toFloat()) * alturaUtil
            val alturaGasto = (punto.gastos.toFloat() / maximo.toFloat()) * alturaUtil

            val xIngreso = baseX + anchoGrupo / 2f - anchoBarra - separacion / 2f
            val xGasto = baseX + anchoGrupo / 2f + separacion / 2f

            drawRoundRect(
                color = colorIngreso,
                topLeft = Offset(xIngreso, size.height - 22f - alturaIngreso),
                size = Size(anchoBarra, alturaIngreso),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = colorGasto,
                topLeft = Offset(xGasto, size.height - 22f - alturaGasto),
                size = Size(anchoBarra, alturaGasto),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            drawContext.canvas.nativeCanvas.drawText(
                punto.etiqueta,
                baseX + anchoGrupo / 2f,
                size.height - 4f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(
                        (colorTexto.red * 255).toInt(),
                        (colorTexto.green * 255).toInt(),
                        (colorTexto.blue * 255).toInt()
                    )
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

/** Anillo de participación por categoría dibujado en Canvas. */
@Composable
private fun GraficoAnillo(categorias: List<ParteCategoria>) {
    val paleta = listOf(
        ColoresWallance.AmarilloPrincipal,
        ColoresWallance.VerdeGanancia,
        ColoresWallance.RojoPerdida,
        Color(0xFF2962FF),
        Color(0xFFAB47BC),
        Color(0xFF26A69A),
        Color(0xFFFF7043),
        Color(0xFF78909C)
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val diametro = minOf(size.width, size.height)
        val grosor = diametro * 0.28f
        val radio = (diametro - grosor) / 2f
        val centro = Offset(size.width / 2f, size.height / 2f)
        val total = categorias.fold(BigDecimal.ZERO) { suma, parte -> suma.add(parte.monto) }
        if (total.signum() <= 0) return@Canvas

        var anguloInicial = -90f
        categorias.forEachIndexed { indice, parte ->
            val fraccion = parte.monto.toFloat() / total.toFloat()
            val barrido = fraccion * 360f
            drawArc(
                color = paleta[indice % paleta.size],
                startAngle = anguloInicial,
                sweepAngle = barrido,
                useCenter = false,
                topLeft = Offset(centro.x - radio, centro.y - radio),
                size = Size(radio * 2f, radio * 2f),
                style = Stroke(width = grosor, cap = StrokeCap.Butt)
            )
            anguloInicial += barrido
        }
    }
}

@Composable
private fun LeyendaCategorias(categorias: List<ParteCategoria>, moneda: String) {
    val paleta = listOf(
        ColoresWallance.AmarilloPrincipal,
        ColoresWallance.VerdeGanancia,
        ColoresWallance.RojoPerdida,
        Color(0xFF2962FF),
        Color(0xFFAB47BC),
        Color(0xFF26A69A),
        Color(0xFFFF7043),
        Color(0xFF78909C)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categorias.forEachIndexed { indice, parte ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .then(
                            Modifier
                        )
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = paleta[indice % paleta.size])
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = parte.categoria?.nombre ?: "Sin categoría",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${parte.porcentaje}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.TextoSecundario
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Utilidades.formatearDinero(parte.monto, moneda),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.TextoPrincipal
                )
            }
        }
    }
}
