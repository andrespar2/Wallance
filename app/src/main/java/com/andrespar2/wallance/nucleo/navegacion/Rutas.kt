package com.andrespar2.wallance.nucleo.navegacion

/**
 * Rutas de navegación de Wallance.
 * Todas las pantallas se identifican con cadenas en español.
 */
object Rutas {
    const val BIENVENIDA = "bienvenida"
    const val INICIO = "inicio"
    const val MOVIMIENTOS = "movimientos"
    const val PRESTAMOS = "prestamos"
    const val ESTADISTICAS = "estadisticas"
    const val CONFIGURACION = "configuracion"

    const val CUENTAS = "cuentas"
    const val CATEGORIAS = "categorias"
    const val PRESUPUESTOS = "presupuestos"
    const val AHORROS = "ahorros"
    const val CONTACTOS = "contactos"
    const val DIVISAS = "divisas"
    const val SUSCRIPCIONES = "suscripciones"
    const val ACERCA_DE = "acerca_de"

    const val MOVIMIENTO_FORM = "movimiento_form"
    const val ARG_MOVIMIENTO_ID = "movimientoId"
    const val ARG_DUPLICAR = "duplicar"
    const val RUTA_MOVIMIENTO_FORM =
        "$MOVIMIENTO_FORM?$ARG_MOVIMIENTO_ID={$ARG_MOVIMIENTO_ID}&$ARG_DUPLICAR={$ARG_DUPLICAR}"

    /** Construye la ruta del formulario para crear, editar o duplicar. */
    fun movimientoForm(movimientoId: Long? = null, duplicar: Boolean = false): String =
        "$MOVIMIENTO_FORM?$ARG_MOVIMIENTO_ID=${movimientoId ?: -1L}&$ARG_DUPLICAR=$duplicar"

    const val CONTACTO_DETALLE = "contacto_detalle"
    const val ARG_CONTACTO_ID = "contactoId"
    const val RUTA_CONTACTO_DETALLE = "$CONTACTO_DETALLE/{$ARG_CONTACTO_ID}"

    const val PRESTAMO_NUEVO = "prestamo_nuevo"
    const val RUTA_PRESTAMO_NUEVO = "$PRESTAMO_NUEVO/{$ARG_CONTACTO_ID}"

    const val PRESTAMO_DETALLE = "prestamo_detalle"
    const val ARG_PRESTAMO_ID = "prestamoId"
    const val RUTA_PRESTAMO_DETALLE = "$PRESTAMO_DETALLE/{$ARG_PRESTAMO_ID}"

    const val META_DETALLE = "meta_detalle"
    const val ARG_META_ID = "metaId"
    const val RUTA_META_DETALLE = "$META_DETALLE/{$ARG_META_ID}"
}
