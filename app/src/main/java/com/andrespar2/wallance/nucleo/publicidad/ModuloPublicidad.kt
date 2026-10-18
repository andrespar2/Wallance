package com.andrespar2.wallance.nucleo.publicidad

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provee el controlador de publicidad de Wallance.
 *
 * Es un objeto sin estado propio (solo controla la inicialización del SDK
 * Start.io), por lo que se entrega siempre la misma instancia.
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloPublicidad {

    @Provides
    @Singleton
    fun controladorPublicidad(): ControladorPublicidad = ControladorPublicidad
}
