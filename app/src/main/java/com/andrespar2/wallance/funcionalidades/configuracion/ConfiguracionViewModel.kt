package com.andrespar2.wallance.funcionalidades.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Preferencias visibles desde la pantalla de configuración.
 */
@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val preferencias: RepositorioPreferencias
) : ViewModel() {

    val monedaPrincipal = preferencias.monedaPrincipal
        .stateIn(viewModelScope, SharingStarted.Eagerly, "COP")

    val ocultarMontos = preferencias.ocultarMontos
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val notificacionesHabilitadas = preferencias.notificacionesHabilitadas
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val consentimientoPublicidad = preferencias.consentimientoPublicidad
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val modoPruebaPublicidad = preferencias.modoPruebaPublicidad
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun fijarOcultarMontos(ocultar: Boolean) {
        viewModelScope.launch { preferencias.fijarOcultarMontos(ocultar) }
    }

    fun fijarNotificaciones(habilitadas: Boolean) {
        viewModelScope.launch { preferencias.fijarNotificaciones(habilitadas) }
    }

    fun fijarMonedaPrincipal(codigo: String) {
        viewModelScope.launch { preferencias.fijarMonedaPrincipal(codigo) }
    }
}
