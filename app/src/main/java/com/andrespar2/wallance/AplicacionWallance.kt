package com.andrespar2.wallance

import android.app.Application
import com.andrespar2.wallance.nucleo.publicidad.ControladorPublicidad
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Clase principal de la aplicación Wallance.
 *
 * Inicializa los componentes globales: base de datos, control de publicidad
 * y tareas en segundo plano.
 */
@HiltAndroidApp
class AplicacionWallance : Application() {

    @Inject
    lateinit var controladorPublicidad: ControladorPublicidad

    override fun onCreate() {
        super.onCreate()
        controladorPublicidad.inicializar(this)
    }
}
