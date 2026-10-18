package com.andrespar2.wallance.nucleo.diseno

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Paleta de colores Black Modern AMOLED de Wallance.
 *
 * Identidad visual propia inspirada en la estética de aplicaciones
 * financieras modernas, sin reproducir recursos de terceros.
 */
object ColoresWallance {
    val NegroAmoled = Color(0xFF000000)
    val NegroSecundario = Color(0xFF0B0E11)
    val Tarjeta = Color(0xFF181A20)
    val SuperficieElevada = Color(0xFF1E2329)
    val AmarilloPrincipal = Color(0xFFFCD535)
    val VerdeGanancia = Color(0xFF0ECB81)
    val RojoPerdida = Color(0xFFF6465D)
    val TextoPrincipal = Color(0xFFFFFFFF)
    val TextoSecundario = Color(0xFF848E9C)
    val BordeSutil = Color(0xFF2B3139)
}

/**
 * Colores semánticos financieros de Wallance.
 */
val ColorGanancia: Color = ColoresWallance.VerdeGanancia
val ColorPerdida: Color = ColoresWallance.RojoPerdida
val ColorNeutro: Color = ColoresWallance.TextoSecundario

private fun esquemaWallance() = darkColorScheme(
    primary = ColoresWallance.AmarilloPrincipal,
    onPrimary = ColoresWallance.NegroAmoled,
    secondary = ColoresWallance.VerdeGanancia,
    onSecondary = ColoresWallance.NegroAmoled,
    tertiary = ColoresWallance.RojoPerdida,
    onTertiary = ColoresWallance.TextoPrincipal,
    background = ColoresWallance.NegroAmoled,
    onBackground = ColoresWallance.TextoPrincipal,
    surface = ColoresWallance.NegroSecundario,
    onSurface = ColoresWallance.TextoPrincipal,
    surfaceVariant = ColoresWallance.Tarjeta,
    onSurfaceVariant = ColoresWallance.TextoSecundario,
    outline = ColoresWallance.BordeSutil,
    error = ColoresWallance.RojoPerdida,
    onError = ColoresWallance.TextoPrincipal
)

private val tipografiaWallance = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

/**
 * Tema visual de Wallance. Es siempre oscuro: la identidad de la aplicación
 * es negro AMOLED puro.
 */
@Composable
fun TemaWallance(contenido: @Composable () -> Unit) {
    // Wallance es exclusivamente oscuro; se ignora el tema del sistema.
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = esquemaWallance(),
        typography = tipografiaWallance,
        content = contenido
    )
}
