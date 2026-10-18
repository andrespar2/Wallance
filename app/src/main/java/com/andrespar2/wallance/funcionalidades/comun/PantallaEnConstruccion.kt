package com.andrespar2.wallance.funcionalidades.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrespar2.wallance.nucleo.diseno.ColoresWallance

/**
 * Pantalla provisional mostrada mientras una sección se termina de construir.
 */
@Composable
fun PantallaEnConstruccion(
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = ColoresWallance.TextoPrincipal,
            textAlign = TextAlign.Center
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresWallance.TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
