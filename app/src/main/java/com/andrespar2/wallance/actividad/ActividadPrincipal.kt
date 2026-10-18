package com.andrespar2.wallance.actividad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrespar2.wallance.nucleo.diseno.TemaWallance
import com.andrespar2.wallance.nucleo.navegacion.NavegacionWallance
import com.andrespar2.wallance.sesion.SesionViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad principal de Wallance. Aloja la navegación de toda la aplicación.
 */
@AndroidEntryPoint
class ActividadPrincipal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TemaWallance {
                ContenidoRaiz()
            }
        }
    }
}

/**
 * Contenido raíz: prepara la sesión (siembra de categorías y preferencias de
 * publicidad) y monta el grafo de navegación.
 */
@Composable
private fun ContenidoRaiz(sesion: SesionViewModel = hiltViewModel()) {
    val consentimiento by sesion.consentimientoPublicidad.collectAsStateWithLifecycle()
    val modoPrueba by sesion.modoPruebaPublicidad.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(consentimiento, modoPrueba) {
        com.andrespar2.wallance.nucleo.publicidad.ControladorPublicidad.fijarModoPrueba(modoPrueba)
    }

    NavegacionWallance(sesion = sesion)
}
