package com.andrespar2.wallance.nucleo.basedatos

import androidx.room.TypeConverter
import com.andrespar2.wallance.nucleo.basedatos.entidades.*
import java.math.BigDecimal

/**
 * Convertidores de Room.
 *
 * Las cantidades monetarias se almacenan como texto decimal para conservar
 * la precisión exacta de [BigDecimal]; nunca se usan tipos de punto flotante.
 */
class ConvertidoresRoom {

    @TypeConverter
    fun deBigDecimal(valor: BigDecimal?): String? = valor?.toPlainString()

    @TypeConverter
    fun aBigDecimal(valor: String?): BigDecimal? = valor?.let { BigDecimal(it) }

    @TypeConverter
    fun deTipoMovimiento(valor: TipoMovimiento): String = valor.name

    @TypeConverter
    fun aTipoMovimiento(valor: String): TipoMovimiento = TipoMovimiento.valueOf(valor)

    @TypeConverter
    fun deTipoCategoria(valor: TipoCategoria): String = valor.name

    @TypeConverter
    fun aTipoCategoria(valor: String): TipoCategoria = TipoCategoria.valueOf(valor)

    @TypeConverter
    fun deTipoCuenta(valor: TipoCuenta): String = valor.name

    @TypeConverter
    fun aTipoCuenta(valor: String): TipoCuenta = TipoCuenta.valueOf(valor)

    @TypeConverter
    fun deTipoPrestamo(valor: TipoPrestamo): String = valor.name

    @TypeConverter
    fun aTipoPrestamo(valor: String): TipoPrestamo = TipoPrestamo.valueOf(valor)

    @TypeConverter
    fun deTipoInteres(valor: TipoInteres): String = valor.name

    @TypeConverter
    fun aTipoInteres(valor: String): TipoInteres = TipoInteres.valueOf(valor)

    @TypeConverter
    fun dePeriodicidadTasa(valor: PeriodicidadTasa): String = valor.name

    @TypeConverter
    fun aPeriodicidadTasa(valor: String): PeriodicidadTasa = PeriodicidadTasa.valueOf(valor)

    @TypeConverter
    fun deSistemaAmortizacion(valor: SistemaAmortizacion): String = valor.name

    @TypeConverter
    fun aSistemaAmortizacion(valor: String): SistemaAmortizacion =
        SistemaAmortizacion.valueOf(valor)

    @TypeConverter
    fun deFrecuenciaCuota(valor: FrecuenciaCuota): String = valor.name

    @TypeConverter
    fun aFrecuenciaCuota(valor: String): FrecuenciaCuota = FrecuenciaCuota.valueOf(valor)

    @TypeConverter
    fun deEstadoPrestamo(valor: EstadoPrestamo): String = valor.name

    @TypeConverter
    fun aEstadoPrestamo(valor: String): EstadoPrestamo = EstadoPrestamo.valueOf(valor)

    @TypeConverter
    fun deEstadoCuota(valor: EstadoCuota): String = valor.name

    @TypeConverter
    fun aEstadoCuota(valor: String): EstadoCuota = EstadoCuota.valueOf(valor)

    @TypeConverter
    fun deTipoPago(valor: TipoPago): String = valor.name

    @TypeConverter
    fun aTipoPago(valor: String): TipoPago = TipoPago.valueOf(valor)

    @TypeConverter
    fun deTipoPeriodoPresupuesto(valor: TipoPeriodoPresupuesto): String = valor.name

    @TypeConverter
    fun aTipoPeriodoPresupuesto(valor: String): TipoPeriodoPresupuesto =
        TipoPeriodoPresupuesto.valueOf(valor)

    @TypeConverter
    fun deEstadoSuscripcion(valor: EstadoSuscripcion): String = valor.name

    @TypeConverter
    fun aEstadoSuscripcion(valor: String): EstadoSuscripcion =
        EstadoSuscripcion.valueOf(valor)
}
