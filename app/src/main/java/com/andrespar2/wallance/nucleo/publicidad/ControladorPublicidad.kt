package com.andrespar2.wallance.nucleo.publicidad

import android.app.Activity
import android.content.Context
import com.startapp.sdk.adsbase.StartAppSDK

/**
 * Controla la integración con la red de publicidad de la aplicación.
 *
 * Reglas de Wallance:
 * - El SDK se inicializa una sola vez.
 * - Se desactivan anuncios de retorno y de inicio.
 * - Si la red no devuelve inventario, la aplicación continúa funcionando
 *   con normalidad (una respuesta sin anuncio no es un error de la app).
 * - La publicidad nunca aparece en formularios sensibles de préstamos,
 *   seguridad ni confirmaciones de pago.
 */
object ControladorPublicidad {

    /** Identificador de la aplicación Wallance ante la red de publicidad. */
    const val ID_APLICACION = "209590896"

    /** Identificador de cuenta (referencia administrativa). */
    const val ID_CUENTA = "124493456"

    @Volatile
    private var inicializado = false

    /** Indica si el SDK ya fue inicializado en este proceso. */
    val estaInicializado: Boolean get() = inicializado

    /**
     * Inicializa el SDK de publicidad de forma segura y única.
     * Cualquier fallo se registra sin bloquear a la aplicación.
     */
    fun inicializar(contexto: Context) {
        if (inicializado) return
        synchronized(this) {
            if (inicializado) return
            try {
                // Se desactivan anuncios de retorno y de inicio: Wallance solo
                // solicita formatos visibles de forma explícita.
                StartAppSDK.enableReturnAds(false)
                StartAppSDK.init(contexto, ID_APLICACION, false)
                inicializado = true
            } catch (error: Throwable) {
                // Una falla del SDK publicitario nunca debe impedir el uso
                // de Wallance.
                android.util.Log.w("WallancePublicidad", "No fue posible inicializar la red de publicidad", error)
            }
        }
    }

    /**
     * Método auxiliar para pantallas: garantiza la inicialización antes de
     * solicitar anuncios.
     */
    fun asegurarInicializacion(contexto: Context) {
        inicializar(contexto.applicationContext)
    }

    @Suppress("UNUSED_PARAMETER")
    fun actividadDisponible(activity: Activity?) = Unit

    /**
     * Activa o desactiva el modo de prueba del SDK. Solo debe usarse en
     * compilaciones de depuración; la versión de producción lo mantiene
     * desactivado.
     */
    fun fijarModoPrueba(activo: Boolean) {
        try {
            StartAppSDK.setTestAdsEnabled(activo)
        } catch (error: Throwable) {
            android.util.Log.w("WallancePublicidad", "No fue posible cambiar el modo de prueba", error)
        }
    }

    /**
     * Registra el consentimiento del usuario para la recopilación de datos
     * publicitarios, cuando corresponde según la jurisdicción.
     */
    fun fijarConsentimiento(contexto: Context, otorgado: Boolean) {
        try {
            StartAppSDK.enableConsent(contexto.applicationContext, otorgado)
        } catch (error: Throwable) {
            android.util.Log.w("WallancePublicidad", "No fue posible registrar el consentimiento", error)
        }
    }
}
