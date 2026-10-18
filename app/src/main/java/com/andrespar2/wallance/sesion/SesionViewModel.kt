package com.andrespar2.wallance.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import com.andrespar2.wallance.nucleo.preferencias.RepositorioPreferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de sesión compartido por todas las pantallas: moneda principal,
 * preferencias de privacidad y estado de la configuración inicial.
 */
@HiltViewModel
class SesionViewModel @Inject constructor(
    private val preferencias: RepositorioPreferencias,
    private val categorias: RepositorioCategorias
) : ViewModel() {

    val monedaPrincipal = preferencias.monedaPrincipal
        .stateIn(viewModelScope, SharingStarted.Eagerly, "COP")

    val configuracionCompletada = preferencias.configuracionCompletada
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val ocultarMontos = preferencias.ocultarMontos
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pinHabilitado = preferencias.pinHabilitado
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val biometriaHabilitada = preferencias.biometriaHabilitada
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val notificacionesHabilitadas = preferencias.notificacionesHabilitadas
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val consentimientoPublicidad = preferencias.consentimientoPublicidad
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val modoPruebaPublicidad = preferencias.modoPruebaPublicidad
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Siembra las categorías predeterminadas la primera vez. */
    fun prepararCatalogoInicial() {
        viewModelScope.launch { categorias.siembraInicial() }
    }

    fun fijarMonedaPrincipal(codigo: String) {
        viewModelScope.launch { preferencias.fijarMonedaPrincipal(codigo) }
    }

    fun marcarConfiguracionCompletada() {
        viewModelScope.launch {
            categorias.siembraInicial()
            preferencias.marcarConfiguracionCompletada()
        }
    }

    fun fijarOcultarMontos(ocultar: Boolean) {
        viewModelScope.launch { preferencias.fijarOcultarMontos(ocultar) }
    }

    fun fijarConsentimientoPublicidad(otorgado: Boolean) {
        viewModelScope.launch { preferencias.fijarConsentimientoPublicidad(otorgado) }
    }

    fun fijarNotificaciones(habilitadas: Boolean) {
        viewModelScope.launch { preferencias.fijarNotificaciones(habilitadas) }
    }

    fun fijarModoPruebaPublicidad(activo: Boolean) {
        viewModelScope.launch { preferencias.fijarModoPruebaPublicidad(activo) }
    }
}
