package com.andrespar2.wallance.nucleo.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias_wallance")

/**
 * Preferencias de Wallance almacenadas con DataStore.
 *
 * Contiene la configuraciÃ³n del usuario y los indicadores de estado
 * (configuraciÃ³n inicial completada, seguridad, divisas y consentimiento
 * publicitario).
 */
@Singleton
class RepositorioPreferencias @Inject constructor(
    @ApplicationContext private val contexto: Context
) {

    private val llaves = object {
        val monedaPrincipal = stringPreferencesKey("moneda_principal")
        val configuracionCompletada = booleanPreferencesKey("configuracion_completada")
        val ocultarMontos = booleanPreferencesKey("ocultar_montos")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinHabilitado = booleanPreferencesKey("pin_habilitado")
        val biometriaHabilitada = booleanPreferencesKey("biometria_habilitada")
        val bloqueoAutomaticoMinutos = longPreferencesKey("bloqueo_automatico_minutos")
        val notificacionesHabilitadas = booleanPreferencesKey("notificaciones_habilitadas")
        val consentimientoPublicidad = booleanPreferencesKey("consentimiento_publicidad")
        val ultimaActualizacionDivisas = longPreferencesKey("ultima_actualizacion_divisas")
        val modoPruebaPublicidad = booleanPreferencesKey("modo_prueba_publicidad")
        val formato12Horas = booleanPreferencesKey("formato_12_horas")
    }

    val monedaPrincipal: Flow<String> = contexto.dataStore.data.map { it[llaves.monedaPrincipal] ?: "COP" }

    val configuracionCompletada: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.configuracionCompletada] ?: false }

    val ocultarMontos: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.ocultarMontos] ?: false }

    val pinHabilitado: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.pinHabilitado] ?: false }

    val pinHash: Flow<String> =
        contexto.dataStore.data.map { it[llaves.pinHash] ?: "" }

    val biometriaHabilitada: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.biometriaHabilitada] ?: false }

    val bloqueoAutomaticoMinutos: Flow<Long> =
        contexto.dataStore.data.map { it[llaves.bloqueoAutomaticoMinutos] ?: 1L }

    val notificacionesHabilitadas: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.notificacionesHabilitadas] ?: true }

    val consentimientoPublicidad: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.consentimientoPublicidad] ?: false }

    val ultimaActualizacionDivisas: Flow<Long> =
        contexto.dataStore.data.map { it[llaves.ultimaActualizacionDivisas] ?: 0L }

    val modoPruebaPublicidad: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.modoPruebaPublicidad] ?: true }

    val formato12Horas: Flow<Boolean> =
        contexto.dataStore.data.map { it[llaves.formato12Horas] ?: false }

    suspend fun fijarMonedaPrincipal(codigo: String) {
        contexto.dataStore.edit { it[llaves.monedaPrincipal] = codigo }
    }

    suspend fun marcarConfiguracionCompletada() {
        contexto.dataStore.edit { it[llaves.configuracionCompletada] = true }
    }

    suspend fun fijarOcultarMontos(ocultar: Boolean) {
        contexto.dataStore.edit { it[llaves.ocultarMontos] = ocultar }
    }

    suspend fun fijarPin(pinHash: String?) {
        contexto.dataStore.edit { preferencias ->
            if (pinHash.isNullOrBlank()) {
                preferencias.remove(llaves.pinHash)
                preferencias[llaves.pinHabilitado] = false
            } else {
                preferencias[llaves.pinHash] = pinHash
                preferencias[llaves.pinHabilitado] = true
            }
        }
    }

    suspend fun fijarBiometria(habilitada: Boolean) {
        contexto.dataStore.edit { it[llaves.biometriaHabilitada] = habilitada }
    }

    suspend fun fijarBloqueoAutomatico(minutos: Long) {
        contexto.dataStore.edit { it[llaves.bloqueoAutomaticoMinutos] = minutos }
    }

    suspend fun fijarNotificaciones(habilitadas: Boolean) {
        contexto.dataStore.edit { it[llaves.notificacionesHabilitadas] = habilitadas }
    }

    suspend fun fijarConsentimientoPublicidad(otorgado: Boolean) {
        contexto.dataStore.edit { it[llaves.consentimientoPublicidad] = otorgado }
    }

    suspend fun fijarUltimaActualizacionDivisas(milis: Long) {
        contexto.dataStore.edit { it[llaves.ultimaActualizacionDivisas] = milis }
    }

    suspend fun fijarModoPruebaPublicidad(activo: Boolean) {
        contexto.dataStore.edit { it[llaves.modoPruebaPublicidad] = activo }
    }

    suspend fun fijarFormato12Horas(usado: Boolean) {
        contexto.dataStore.edit { it[llaves.formato12Horas] = usado }
    }
}

