package com.andrespar2.wallance.nucleo.basedatos

import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria

/**
 * Catálogo de categorías predeterminadas de Wallance.
 *
 * Se insertan únicamente la primera vez que se usa la aplicación. El usuario
 * puede editarlas, archivarlas o crear categorías propias sin que se pierdan
 * los movimientos asociados.
 */
object Semillas {

    val CATEGORIAS_GASTO: List<Pair<String, String>> = listOf(
        "Alimentación" to "alimentacion",
        "Restaurantes" to "restaurante",
        "Supermercado" to "supermercado",
        "Transporte" to "transporte",
        "Gasolina" to "gasolina",
        "Vivienda" to "vivienda",
        "Arriendo" to "arriendo",
        "Servicios públicos" to "servicios",
        "Agua" to "agua",
        "Electricidad" to "electricidad",
        "Gas" to "gas",
        "Internet" to "internet",
        "Telefonía" to "telefono",
        "Salud" to "salud",
        "Medicamentos" to "medicamentos",
        "Educación" to "educacion",
        "Entretenimiento" to "entretenimiento",
        "Videojuegos" to "videojuegos",
        "Suscripciones" to "suscripcion",
        "Compras" to "compras",
        "Ropa" to "ropa",
        "Mascotas" to "mascotas",
        "Viajes" to "viajes",
        "Impuestos" to "impuestos",
        "Comisiones" to "comisiones",
        "Regalos" to "regalos",
        "Mantenimiento" to "mantenimiento",
        "Otros" to "otros"
    )

    val CATEGORIAS_INGRESO: List<Pair<String, String>> = listOf(
        "Salario" to "salario",
        "Trabajo independiente" to "independiente",
        "Ventas" to "ventas",
        "Negocios" to "negocios",
        "Comisiones" to "comisiones",
        "Bonificaciones" to "bonificacion",
        "Intereses" to "intereses",
        "Dividendos" to "dividendos",
        "Alquileres" to "alquileres",
        "Regalos recibidos" to "regalo",
        "Reembolsos" to "reembolso",
        "Otros ingresos" to "otros_ingresos"
    )

    /** Construye la lista completa de categorías iniciales. */
    fun categoriasIniciales(): List<Categoria> {
        val gastos = CATEGORIAS_GASTO.mapIndexed { indice, (nombre, icono) ->
            Categoria(
                nombre = nombre,
                tipo = TipoCategoria.GASTO,
                icono = icono,
                color = 0xFFF6465D,
                esPredeterminada = true,
                orden = indice
            )
        }
        val ingresos = CATEGORIAS_INGRESO.mapIndexed { indice, (nombre, icono) ->
            Categoria(
                nombre = nombre,
                tipo = TipoCategoria.INGRESO,
                icono = icono,
                color = 0xFF0ECB81,
                esPredeterminada = true,
                orden = indice
            )
        }
        return gastos + ingresos
    }
}
