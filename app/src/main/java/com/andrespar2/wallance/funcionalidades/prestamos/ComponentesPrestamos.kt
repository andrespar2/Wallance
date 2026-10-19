package com.andrespar2.wallance.funcionalidades.prestamos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.Pago
import com.andrespar2.wallance.nucleo.basedatos.entidades.PeriodicidadTasa
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.SistemaAmortizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoInteres
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPago
import com.andrespar2.wallance.nucleo.basedatos.repositorios.CuentaConSaldo
import com.andrespar2.wallance.nucleo.comun.Utilidades
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPeligro
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPrincipal
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonSecundario
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.FilaDato
import com.andrespar2.wallance.nucleo.diseno.componentes.ModalBottomSheetFlujo
import com.andrespar2.wallance.nucleo.diseno.componentes.Separator
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorFecha
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorPorHoja
import com.andrespar2.wallance.nucleo.diseno.componentes.TarjetaWallance
import com.andrespar2.wallance.nucleo.finanzas.ResumenAmortizacion
import java.math.BigDecimal

/** Color de acento según el estado del préstamo. */
private fun colorDeEstado(estado: EstadoPrestamo) = when (estado) {
    EstadoPrestamo.ACTIVO -> ColoresWallance.AmarilloPrincipal
    EstadoPrestamo.PAGADO -> ColoresWallance.VerdeGanancia
    EstadoPrestamo.VENCIDO -> ColoresWallance.RojoPerdida
    EstadoPrestamo.CANCELADO -> ColoresWallance.TextoSecundario
    EstadoPrestamo.REESTRUCTURADO -> ColoresWallance.TextoSecundario
}

@Composable
fun TarjetaPrestamo(
    item: ItemPrestamo,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val prestamo = item.prestamo
    val resumen = item.resumen
    val moneda = prestamo.moneda
    TarjetaWallance(modifier = Modifier.fillMaxWidth().clickable(onClick = onAbrir)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.contacto?.nombreCompleto ?: "Contacto eliminado",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresWallance.TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = prestamo.estado.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorDeEstado(prestamo.estado)
                )
            }
            Text(
                text = prestamo.tipo.etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = ColoresWallance.TextoSecundario
            )
            Spacer(modifier = Modifier.height(4.dp))
            FilaDato(
                etiqueta = "Capital",
                valor = Utilidades.formatearDinero(prestamo.capital, moneda)
            )
            FilaDato(
                etiqueta = "Saldo pendiente",
                valor = Utilidades.formatearDinero(resumen.capitalPendiente, moneda),
                colorValor = if (resumen.capitalPendiente.signum() > 0) {
                    ColoresWallance.TextoPrincipal
                } else {
                    ColoresWallance.VerdeGanancia
                }
            )
            FilaDato(
                etiqueta = "Cuotas",
                valor = "${resumen.cuotasPagadas}/${prestamo.numeroCuotas} pagadas"
            )
            if (resumen.cuotasVencidas > 0) {
                FilaDato(
                    etiqueta = "Cuotas vencidas",
                    valor = resumen.cuotasVencidas.toString(),
                    colorValor = ColoresWallance.RojoPerdida
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar préstamo",
                        tint = ColoresWallance.TextoSecundario
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar préstamo",
                        tint = ColoresWallance.RojoPerdida
                    )
                }
            }
        }
    }
}

@Composable
fun FormularioContacto(
    contacto: Contacto,
    error: String?,
    onGuardar: (String, String, String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(contacto.id) { mutableStateOf(contacto.nombre) }
    var apellido by remember(contacto.id) { mutableStateOf(contacto.apellido) }
    var telefono by remember(contacto.id) { mutableStateOf(contacto.telefono) }
    var correo by remember(contacto.id) { mutableStateOf(contacto.correo) }
    var notas by remember(contacto.id) { mutableStateOf(contacto.notas) }

    ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (contacto.id == 0L) "Nuevo contacto" else "Editar contacto"
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CampoTexto(
                valor = nombre,
                onValorCambiado = { nombre = it },
                etiqueta = "Nombre *"
            )
            CampoTexto(
                valor = apellido,
                onValorCambiado = { apellido = it },
                etiqueta = "Apellido"
            )
            CampoTexto(
                valor = telefono,
                onValorCambiado = { telefono = it },
                etiqueta = "Teléfono"
            )
            CampoTexto(
                valor = correo,
                onValorCambiado = { correo = it },
                etiqueta = "Correo electrónico"
            )
            CampoTexto(
                valor = notas,
                onValorCambiado = { notas = it },
                etiqueta = "Notas",
                unaLinea = false
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.RojoPerdida
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonSecundario(
                    texto = "Cancelar",
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f)
                )
                BotonPrincipal(
                    texto = "Guardar",
                    onClick = { onGuardar(nombre, apellido, telefono, correo, notas) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FormularioPrestamo(
    prestamo: Prestamo,
    contacto: Contacto?,
    contactos: List<Contacto>,
    vistaPrevia: ResumenAmortizacion?,
    error: String?,
    onCambio: ((Prestamo) -> Prestamo) -> Unit,
    onContactoCambiado: (Long) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    var capitalTexto by remember(prestamo.id) {
        mutableStateOf(
            if (prestamo.capital.signum() == 0) "" else prestamo.capital.toPlainString()
        )
    }
    var tasaTexto by remember(prestamo.id) {
        mutableStateOf(
            if (prestamo.tasaValor.signum() == 0) "" else prestamo.tasaValor.toPlainString()
        )
    }
    var cuotasTexto by remember(prestamo.id) { mutableStateOf(prestamo.numeroCuotas.toString()) }
    var moraTexto by remember(prestamo.id) {
        mutableStateOf(
            if (prestamo.moraTasa.signum() == 0) "" else prestamo.moraTasa.toPlainString()
        )
    }

    ModalBottomSheetFlujo(
        onDismissRequest = onCancelar,
        titulo = if (prestamo.id == 0L) "Nuevo préstamo" else "Editar préstamo"
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SelectorPorHoja(
                valor = prestamo.contactoId,
                opciones = contactos.map { it.id },
                etiqueta = "Contacto",
                textoValor = contacto?.nombreCompleto ?: "Seleccionar",
                onSeleccion = onContactoCambiado,
                textoDe = { id -> contactos.firstOrNull { it.id == id }?.nombreCompleto ?: id.toString() }
            )
            CampoTexto(
                valor = capitalTexto,
                onValorCambiado = {
                    capitalTexto = it
                    onCambio { p -> p.copy(capital = Utilidades.textoADinero(it) ?: BigDecimal.ZERO) }
                },
                etiqueta = "Capital prestado",
                esDinero = true
            )
            SelectorPorHoja(
                valor = prestamo.tasaTipo,
                opciones = TipoInteres.entries,
                etiqueta = "Tipo de interés",
                textoDe = { it.etiqueta },
                onSeleccion = { onCambio { p -> p.copy(tasaTipo = it) } }
            )
            if (prestamo.tasaTipo != TipoInteres.SIN_INTERES) {
                CampoTexto(
                    valor = tasaTexto,
                    onValorCambiado = {
                        tasaTexto = it
                        onCambio { p -> p.copy(tasaValor = Utilidades.textoADinero(it) ?: BigDecimal.ZERO) }
                    },
                    etiqueta = "Tasa (%)",
                    esDinero = true
                )
                SelectorPorHoja(
                    valor = prestamo.tasaPeriodicidad,
                    opciones = PeriodicidadTasa.entries,
                    etiqueta = "Periodicidad de la tasa",
                    textoDe = { it.etiqueta },
                    onSeleccion = { onCambio { p -> p.copy(tasaPeriodicidad = it) } }
                )
            }
            SelectorPorHoja(
                valor = prestamo.sistemaAmortizacion,
                opciones = SistemaAmortizacion.entries,
                etiqueta = "Sistema de amortización",
                textoDe = { it.etiqueta },
                onSeleccion = { onCambio { p -> p.copy(sistemaAmortizacion = it) } }
            )
            SelectorPorHoja(
                valor = prestamo.frecuenciaCuotas,
                opciones = FrecuenciaCuota.entries,
                etiqueta = "Frecuencia de cuotas",
                textoDe = { it.etiqueta },
                onSeleccion = { onCambio { p -> p.copy(frecuenciaCuotas = it) } }
            )
            CampoTexto(
                valor = cuotasTexto,
                onValorCambiado = {
                    cuotasTexto = it
                    onCambio { p -> p.copy(numeroCuotas = it.toIntOrNull()?.coerceAtLeast(1) ?: 1) }
                },
                etiqueta = "Número de cuotas"
            )
            SelectorFecha(
                valor = prestamo.fechaDesembolso,
                etiqueta = "Fecha de desembolso",
                onSeleccion = { onCambio { p -> p.copy(fechaDesembolso = it) } }
            )
            SelectorFecha(
                valor = prestamo.fechaVencimiento,
                etiqueta = "Fecha de vencimiento",
                onSeleccion = { onCambio { p -> p.copy(fechaVencimiento = it) } }
            )
            Separator()
            FilaInterruptorSimple(
                texto = "Aplicar mora por atraso",
                activo = prestamo.moraHabilitada,
                onCambio = { onCambio { p -> p.copy(moraHabilitada = it) } }
            )
            if (prestamo.moraHabilitada) {
                CampoTexto(
                    valor = moraTexto,
                    onValorCambiado = {
                        moraTexto = it
                        onCambio { p -> p.copy(moraTasa = Utilidades.textoADinero(it) ?: BigDecimal.ZERO) }
                    },
                    etiqueta = "Mora diaria (%)",
                    esDinero = true
                )
            }
            if (vistaPrevia != null) {
                Separator()
                TarjetaWallance(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Vista previa del cronograma",
                            style = MaterialTheme.typography.titleSmall,
                            color = ColoresWallance.AmarilloPrincipal
                        )
                        FilaDato(
                            etiqueta = "Cuota de referencia",
                            valor = Utilidades.formatearDinero(vistaPrevia.cuotaReferencia, prestamo.moneda)
                        )
                        FilaDato(
                            etiqueta = "Intereses previstos",
                            valor = Utilidades.formatearDinero(vistaPrevia.interesesPrevistos, prestamo.moneda)
                        )
                        FilaDato(
                            etiqueta = "Total a pagar",
                            valor = Utilidades.formatearDinero(vistaPrevia.totalPrevisto, prestamo.moneda),
                            resaltado = true
                        )
                    }
                }
            }
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.RojoPerdida
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonSecundario(
                    texto = "Cancelar",
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f)
                )
                BotonPrincipal(
                    texto = "Guardar",
                    onClick = onGuardar,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilaInterruptorSimple(
    texto: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCambio(!activo) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresWallance.TextoPrincipal,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = ColoresWallance.AmarilloPrincipal,
                checkedThumbColor = ColoresWallance.NegroAmoled
            )
        )
    }
}

@Composable
fun ModalDetallePrestamo(
    prestamo: Prestamo,
    contacto: Contacto?,
    cuotas: List<Cuota>,
    pagos: List<Pago>,
    cuentas: List<CuentaConSaldo>,
    error: String?,
    onRegistrarPago: (String, Long?, TipoPago) -> Unit,
    onCancelar: () -> Unit,
    onCerrar: () -> Unit
) {
    var montoTexto by remember { mutableStateOf("") }
    var cuentaId by remember { mutableStateOf<Long?>(null) }
    var tipoPago by remember { mutableStateOf(TipoPago.ABONO) }

    ModalBottomSheetFlujo(
        onDismissRequest = onCerrar,
        titulo = contacto?.nombreCompleto ?: "Detalle del préstamo"
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilaDato(
                etiqueta = "Estado",
                valor = prestamo.estado.etiqueta,
                colorValor = colorDeEstado(prestamo.estado)
            )
            FilaDato(
                etiqueta = "Capital",
                valor = Utilidades.formatearDinero(prestamo.capital, prestamo.moneda)
            )
            FilaDato(
                etiqueta = "Sistema",
                valor = prestamo.sistemaAmortizacion.etiqueta
            )
            FilaDato(
                etiqueta = "Interés",
                valor = prestamo.tasaTipo.etiqueta
            )
            Separator()
            Text(
                text = "Registrar pago",
                style = MaterialTheme.typography.titleSmall,
                color = ColoresWallance.AmarilloPrincipal
            )
            CampoTexto(
                valor = montoTexto,
                onValorCambiado = { montoTexto = it },
                etiqueta = "Monto del pago",
                esDinero = true
            )
            SelectorPorHoja(
                valor = tipoPago,
                opciones = TipoPago.entries,
                etiqueta = "Tipo de pago",
                textoDe = { it.etiqueta },
                onSeleccion = { tipoPago = it }
            )
            SelectorPorHoja(
                valor = cuentaId,
                opciones = listOf(null) + cuentas.map { it.cuenta.id },
                etiqueta = "Cuenta (opcional)",
                textoValor = cuentaId?.let { id ->
                    cuentas.firstOrNull { it.cuenta.id == id }?.cuenta?.nombre
                } ?: "Sin cuenta",
                onSeleccion = { cuentaId = it },
                textoDe = { id -> id?.let { i -> cuentas.firstOrNull { it.cuenta.id == i }?.cuenta?.nombre } ?: "Sin cuenta" }
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.RojoPerdida
                )
            }
            BotonPrincipal(
                texto = "Registrar pago",
                onClick = { onRegistrarPago(montoTexto, cuentaId, tipoPago) },
                modifier = Modifier.fillMaxWidth()
            )
            Separator()
            Text(
                text = "Cronograma",
                style = MaterialTheme.typography.titleSmall,
                color = ColoresWallance.AmarilloPrincipal
            )
            cuotas.forEach { cuota ->
                FilaCuota(cuota = cuota, moneda = prestamo.moneda)
            }
            if (pagos.isNotEmpty()) {
                Separator()
                Text(
                    text = "Pagos registrados",
                    style = MaterialTheme.typography.titleSmall,
                    color = ColoresWallance.AmarilloPrincipal
                )
                pagos.forEach { pago ->
                    FilaDato(
                        etiqueta = Utilidades.formatearFechaCorta(pago.fecha),
                        valor = Utilidades.formatearDinero(pago.monto, prestamo.moneda),
                        colorValor = ColoresWallance.VerdeGanancia
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (prestamo.estado != EstadoPrestamo.CANCELADO && prestamo.estado != EstadoPrestamo.PAGADO) {
                BotonPeligro(
                    texto = "Cancelar préstamo",
                    onClick = onCancelar,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilaCuota(cuota: Cuota, moneda: String) {
    val color = when (cuota.estado) {
        EstadoCuota.PAGADA -> ColoresWallance.VerdeGanancia
        EstadoCuota.VENCIDA -> ColoresWallance.RojoPerdida
        EstadoCuota.PARCIAL -> ColoresWallance.AmarilloPrincipal
        EstadoCuota.PENDIENTE -> ColoresWallance.TextoSecundario
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cuota ${cuota.numero} · ${cuota.estado.etiqueta}",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            Text(
                text = Utilidades.formatearFechaCorta(cuota.fechaVencimiento),
                style = MaterialTheme.typography.bodySmall,
                color = ColoresWallance.TextoSecundario
            )
        }
        Text(
            text = Utilidades.formatearDinero(cuota.valorTotal, moneda),
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresWallance.TextoPrincipal
        )
    }
}
