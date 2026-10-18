package com.andrespar2.wallance.nucleo.diseno.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance
import java.math.BigDecimal

/**
 * Componentes visuales reutilizables de Wallance.
 * Tarjetas minimalistas, botones y estados vacíos sobre fondo AMOLED.
 */

@Composable
fun TarjetaWallance(
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ColoresWallance.Tarjeta)
            .padding(16.dp)
    ) {
        contenido()
    }
}

@Composable
fun BotonWallance(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    colorFondo: Color = ColoresWallance.AmarilloPrincipal,
    colorTexto: Color = ColoresWallance.NegroAmoled,
    icono: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            contentColor = colorTexto,
            disabledContainerColor = ColoresWallance.SuperficieElevada,
            disabledContentColor = ColoresWallance.TextoSecundario
        )
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colorTexto,
                strokeWidth = 2.dp
            )
        } else {
            if (icono != null) {
                Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = ColoresWallance.TextoPrincipal)
    }
}

@Composable
fun BotonPeligro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = ColoresWallance.RojoPerdida)
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onValorCambiado: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esDinero: Boolean = false,
    ayuda: String? = null,
    error: String? = null,
    habilitado: Boolean = true,
    unaLinea: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambiado,
        label = { Text(etiqueta) },
        modifier = modifier.fillMaxWidth(),
        enabled = habilitado,
        singleLine = unaLinea,
        isError = error != null,
        supportingText = {
            when {
                error != null -> Text(error, color = ColoresWallance.RojoPerdida)
                ayuda != null -> Text(ayuda, color = ColoresWallance.TextoSecundario)
            }
        },
        keyboardOptions = if (esDinero) {
            KeyboardOptions(keyboardType = KeyboardType.Decimal)
        } else {
            KeyboardOptions.Default
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColoresWallance.AmarilloPrincipal,
            unfocusedBorderColor = ColoresWallance.BordeSutil,
            focusedTextColor = ColoresWallance.TextoPrincipal,
            unfocusedTextColor = ColoresWallance.TextoPrincipal,
            cursorColor = ColoresWallance.AmarilloPrincipal,
            focusedLabelColor = ColoresWallance.AmarilloPrincipal,
            unfocusedLabelColor = ColoresWallance.TextoSecundario,
            errorBorderColor = ColoresWallance.RojoPerdida,
            focusedContainerColor = ColoresWallance.Tarjeta,
            unfocusedContainerColor = ColoresWallance.Tarjeta
        )
    )
}

@Composable
fun EtiquetaMonto(
    cantidad: BigDecimal,
    moneda: String,
    textoFormateado: String,
    modifier: Modifier = Modifier,
    esGanancia: Boolean? = null,
    estilo: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val color = when (esGanancia) {
        true -> ColoresWallance.VerdeGanancia
        false -> ColoresWallance.RojoPerdida
        null -> ColoresWallance.TextoPrincipal
    }
    Text(
        text = if (moneda.isBlank()) textoFormateado else textoFormateado,
        style = estilo,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun FilaDato(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    colorValor: Color = ColoresWallance.TextoPrincipal,
    resaltado: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario
        )
        Text(
            text = valor,
            style = if (resaltado) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyLarge,
            fontWeight = if (resaltado) FontWeight.SemiBold else FontWeight.Normal,
            color = colorValor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = ColoresWallance.TextoSecundario,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = ColoresWallance.TextoPrincipal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (textoAccion != null && onAccion != null) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onAccion) {
                Text(textoAccion, color = ColoresWallance.AmarilloPrincipal)
            }
        }
    }
}

@Composable
fun IndicadorCarga(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ColoresWallance.AmarilloPrincipal)
    }
}

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    IndicadorCarga(modifier = modifier)
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorFondo: Color = ColoresWallance.AmarilloPrincipal,
    colorTexto: Color = ColoresWallance.NegroAmoled,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null
) {
    BotonWallance(
        texto = texto,
        onClick = onClick,
        modifier = modifier,
        habilitado = habilitado,
        cargando = cargando,
        colorFondo = colorFondo,
        colorTexto = colorTexto,
        icono = icono
    )
}

@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String = "Confirmar",
    textoCancelar: String = "Cancelar",
    peligroso: Boolean = false,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = ColoresWallance.SuperficieElevada,
        titleContentColor = ColoresWallance.TextoPrincipal,
        textContentColor = ColoresWallance.TextoSecundario,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    textoConfirmar,
                    color = if (peligroso) ColoresWallance.RojoPerdida else ColoresWallance.AmarilloPrincipal
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(textoCancelar, color = ColoresWallance.TextoSecundario)
            }
        }
    )
}

@Composable
fun PuntoColor(color: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(color))
    )
}

/**
 * Selector desplegable genérico con el estilo de Wallance.
 */
@Composable
fun <T> SelectorDesplegable(
    valor: T,
    opciones: List<T>,
    etiqueta: String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    textoDe: (T) -> String = { it.toString() },
    habilitado: Boolean = true
) {
    var abierto by androidx.compose.runtime.remember { mutableStateOf(false) }
    val fuenteInteracciones = androidx.compose.foundation.interaction.MutableInteractionSource()
    androidx.compose.runtime.LaunchedEffect(fuenteInteracciones) {
        fuenteInteracciones.interactions.collect { interaccion ->
            if (interaccion is androidx.compose.foundation.interaction.PressInteraction.Release) {
                abierto = true
            }
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textoDe(valor),
            onValueChange = {},
            label = { Text(etiqueta) },
            readOnly = true,
            enabled = habilitado,
            interactionSource = fuenteInteracciones,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Desplegar opciones",
                    tint = ColoresWallance.TextoSecundario
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColoresWallance.AmarilloPrincipal,
                unfocusedBorderColor = ColoresWallance.BordeSutil,
                focusedTextColor = ColoresWallance.TextoPrincipal,
                unfocusedTextColor = ColoresWallance.TextoPrincipal,
                focusedLabelColor = ColoresWallance.AmarilloPrincipal,
                unfocusedLabelColor = ColoresWallance.TextoSecundario,
                focusedContainerColor = ColoresWallance.Tarjeta,
                unfocusedContainerColor = ColoresWallance.Tarjeta
            )
        )
        androidx.compose.material3.DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            modifier = Modifier.background(ColoresWallance.SuperficieElevada)
        ) {
            opciones.forEach { opcion ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            textoDe(opcion),
                            color = if (opcion == valor) ColoresWallance.AmarilloPrincipal
                            else ColoresWallance.TextoPrincipal
                        )
                    },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}
