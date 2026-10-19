package com.andrespar2.wallance.nucleo.publicidad

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener

/**
 * Banner publicitario integrado en Jetpack Compose.
 *
 * Comportamiento exigido por Wallance:
 * - No ocupa espacio si la red no devuelve inventario (una respuesta sin
 *   anuncio no es un error de la aplicación): la vista queda oculta y mide
 *   cero, por lo que no deja espacios vacíos.
 * - Se libera el recurso al salir de la pantalla.
 * - La publicidad es parte del funcionamiento de la aplicación gratuita.
 */
@Composable
fun BannerPublicidad(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            ControladorPublicidad.asegurarInicializacion(ctx)
            val escuchador = object : BannerListener {
                override fun onReceiveAd(view: View) {
                    view.visibility = View.VISIBLE
                }

                override fun onFailedToReceiveAd(view: View) {
                    // Sin inventario: se oculta sin dejar espacios vacíos.
                    view.visibility = View.GONE
                }

                override fun onImpression(view: View) = Unit
                override fun onClick(view: View) = Unit
            }
            Banner(ctx, escuchador).apply {
                visibility = View.GONE
                showBanner()
            }
        },
        onRelease = { banner ->
            try {
                banner.hideBanner()
            } catch (error: Throwable) {
                android.util.Log.w("WallancePublicidad", "No fue posible liberar el banner", error)
            }
        }
    )
}
