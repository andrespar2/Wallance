package com.andrespar2.wallance.funcionalidades.bienvenida

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCuenta
import com.andrespar2.wallance.nucleo.comun.etiqueta
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonPeligro
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonSecundario
import com.andrespar2.wallance.nucleo.diseno.componentes.BotonWallance
import com.andrespar2.wallance.nucleo.diseno.componentes.CampoTexto
import com.andrespar2.wallance.nucleo.diseno.componentes.SelectorDesplegable
import com.andrespar2.wallance.nucleo.finanzas.MotorDivisas

/**
 * Asistente de configuración inicial de Wallance.
 * Funciona completamente sin conexión y sin registro en servidores.
 */
@Composable
fun PantallaBienvenida(
    onTerminado: () -> Unit,
    viewModel: BienvenidaViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColoresWallance.NegroAmoled)
            .padding(24.dp)
    ) {
        // Progreso del asistente.
        LinearProgressIndicator(
            progress = { (viewModel.paso + 1f) / viewModel.totalPasos },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ColoresWallance.AmarilloPrincipal,
            trackColor = ColoresWallance.SuperficieElevada
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Paso ${viewModel.paso + 1} de ${viewModel.totalPasos}",
            style = MaterialTheme.typography.labelMedium,
            color = ColoresWallance.TextoSecundario
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (viewModel.paso) {
                0 -> PasoBienvenida()
                1 -> PasoFuncionalidades()
                2 -> PasoMoneda(viewModel)
                3 -> PasoPrimeraCuenta(viewModel)
                4 -> PasoSeguridad(viewModel)
                5 -> PasoNotificaciones(viewModel)
                6 -> PasoPrivacidad(viewModel, onTerminado)
            }

            viewModel.mensajeError?.let { mensaje ->
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColoresWallance.RojoPerdida
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.paso > 0) {
                BotonSecundario(
                    texto = "Atrás",
                    onClick = viewModel::anterior,
                    modifier = Modifier.weight(1f)
                )
            }
            if (viewModel.paso < viewModel.totalPasos - 1) {
                BotonWallance(
                    texto = "Siguiente",
                    onClick = {
                        if (viewModel.validarPasoActual() == null) viewModel.siguiente()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                BotonWallance(
                    texto = "Comenzar",
                    onClick = { viewModel.finalizar(onTerminado) },
                    cargando = viewModel.cargando,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = "Desarrollado por andrespar2",
            style = MaterialTheme.typography.labelMedium,
            color = ColoresWallance.TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun PasoBienvenida() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Wallance",
            style = MaterialTheme.typography.displayMedium,
            color = ColoresWallance.AmarilloPrincipal
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tu dinero. Tu equilibrio.",
            style = MaterialTheme.typography.titleMedium,
            color = ColoresWallance.TextoPrincipal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Wallance es tu administrador integral de finanzas personales. " +
                "Funciona sin conexión, con tus datos guardados únicamente en tu dispositivo.",
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresWallance.TextoSecundario,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PasoFuncionalidades() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "¿Qué puedes hacer con Wallance?",
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal
        )
        TarjetaFuncionalidad(
            icono = Icons.Default.TrendingUp,
            titulo = "Ingresos, gastos y presupuestos",
            detalle = "Registra cada movimiento, organízalo por categorías y controla cuánto consumes al mes."
        )
        TarjetaFuncionalidad(
            icono = Icons.Default.CheckCircle,
            titulo = "Préstamos y contactos",
            detalle = "Lleva el control del dinero que prestas o recibes, con intereses, cuotas y abonos por persona."
        )
        TarjetaFuncionalidad(
            icono = Icons.Default.Security,
            titulo = "Seguridad y privacidad",
            detalle = "Protege la aplicación con PIN o biometría. Nada sale de tu teléfono sin tu permiso."
        )
    }
}

@Composable
private fun TarjetaFuncionalidad(icono: ImageVector, titulo: String, detalle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ColoresWallance.Tarjeta)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = ColoresWallance.AmarilloPrincipal)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = ColoresWallance.TextoPrincipal)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = ColoresWallance.TextoSecundario)
        }
    }
}

@Composable
private fun PasoMoneda(viewModel: BienvenidaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Elige tu moneda principal",
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal
        )
        Text(
            text = "Es la moneda en la que Wallance mostrará tus balances. Podrás registrar movimientos en otras monedas.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario
        )
        SelectorDesplegable(
            valor = viewModel.monedaSeleccionada,
            opciones = MotorDivisas.MONEDAS.map { it.first },
            etiqueta = "Moneda principal",
            onSeleccion = { viewModel.monedaSeleccionada = it },
            textoDe = { codigo ->
                val nombre = MotorDivisas.nombreDeMoneda(codigo)
                "$codigo — $nombre"
            }
        )
    }
}

@Composable
private fun PasoPrimeraCuenta(viewModel: BienvenidaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Crea tu primera cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal
        )
        Text(
            text = "Una cuenta representa donde guardas tu dinero: efectivo, banco, Nequi, billetera digital u otra.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario
        )
        CampoTexto(
            valor = viewModel.nombreCuenta,
            onValorCambiado = { viewModel.nombreCuenta = it },
            etiqueta = "Nombre de la cuenta"
        )
        SelectorDesplegable(
            valor = viewModel.tipoCuenta,
            opciones = TipoCuenta.entries,
            etiqueta = "Tipo de cuenta",
            onSeleccion = { viewModel.tipoCuenta = it },
            textoDe = { it.etiqueta }
        )
        CampoTexto(
            valor = viewModel.saldoInicialTexto,
            onValorCambiado = { viewModel.saldoInicialTexto = it },
            etiqueta = "Saldo inicial",
            esDinero = true,
            ayuda = "Puedes dejarlo en cero y ajustarlo después."
        )
    }
}

@Composable
private fun PasoSeguridad(viewModel: BienvenidaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Protege tus datos",
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal
        )
        Text(
            text = "Puedes definir un PIN de 4 a 6 dígitos. Es opcional: si lo dejas vacío, accederás directamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario
        )
        CampoTexto(
            valor = viewModel.pin,
            onValorCambiado = { valor -> if (valor.length <= 6 && valor.all { it.isDigit() }) viewModel.pin = valor },
            etiqueta = "PIN (opcional)",
            esDinero = true,
            ayuda = "Se guarda cifrado en tu dispositivo."
        )
        FilaInterruptor(
            texto = "Usar huella o rostro del dispositivo",
            detalle = "Si tu teléfono lo permite, podrás abrir Wallance con biometría.",
            activo = viewModel.biometriaActiva,
            onCambio = { viewModel.biometriaActiva = it }
        )
    }
}

@Composable
private fun PasoNotificaciones(viewModel: BienvenidaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = ColoresWallance.AmarilloPrincipal,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Recordatorios",
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal
        )
        Text(
            text = "Wallance puede avisarte sobre cuotas próximas a vencer, presupuestos superados y metas de ahorro.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario
        )
        FilaInterruptor(
            texto = "Activar notificaciones",
            detalle = "Puedes cambiarlo en cualquier momento desde Configuración.",
            activo = viewModel.notificacionesActivas,
            onCambio = { viewModel.notificacionesActivas = it }
        )
    }
}

@Composable
private fun PasoPrivacidad(viewModel: BienvenidaViewModel, onTerminado: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Privacidad",
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal
        )
        Text(
            text = "Wallance muestra publicidad de Start.io para mantener la aplicación gratuita. " +
                "Nunca compartimos tus saldos, contactos ni préstamos con la red publicitaria. " +
                "Si no otorgas tu consentimiento, la aplicación seguirá funcionando sin anuncios.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario
        )
        FilaInterruptor(
            texto = "Acepto la publicidad personalizada",
            detalle = "Puedes modificarlo luego en Configuración → Privacidad.",
            activo = viewModel.consentimientoPublicidad,
            onCambio = { viewModel.consentimientoPublicidad = it }
        )
        BotonPeligro(
            texto = "Continuar sin publicidad",
            onClick = {
                viewModel.consentimientoPublicidad = false
                viewModel.finalizar(onTerminado)
            }
        )
    }
}

@Composable
private fun FilaInterruptor(texto: String, detalle: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ColoresWallance.Tarjeta)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.titleMedium, color = ColoresWallance.TextoPrincipal)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = ColoresWallance.TextoSecundario)
        }
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ColoresWallance.AmarilloPrincipal,
                checkedThumbColor = ColoresWallance.NegroAmoled,
                uncheckedTrackColor = ColoresWallance.SuperficieElevada,
                uncheckedThumbColor = ColoresWallance.TextoSecundario
            )
        )
    }
}
