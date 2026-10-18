package com.andrespar2.wallance.nucleo.basedatos.entidades

/**
 * Tipos de movimiento financiero reconocidos por Wallance.
 *
 * Los movimientos entre cuentas propias (TRANSFERENCIA) nunca se contabilizan
 * como ingresos ni como gastos.
 */
enum class TipoMovimiento {
    INGRESO,
    GASTO,
    TRANSFERENCIA,
    AJUSTE,
    PRESTAMO_OTORGADO,
    PRESTAMO_RECIBIDO,
    COBRO_PRESTAMO,
    PAGO_DEUDA,
    INTERESES_COBRADOS,
    INTERESES_PAGADOS,
    COMISION,
    REEMBOLSO
}

/** Indica si un movimiento representa entrada o salida de dinero. */
val TipoMovimiento.esIngreso: Boolean
    get() = this == TipoMovimiento.INGRESO ||
        this == TipoMovimiento.COBRO_PRESTAMO ||
        this == TipoMovimiento.INTERESES_COBRADOS ||
        this == TipoMovimiento.REEMBOLSO

val TipoMovimiento.esGasto: Boolean
    get() = this == TipoMovimiento.GASTO ||
        this == TipoMovimiento.COMISION ||
        this == TipoMovimiento.PAGO_DEUDA ||
        this == TipoMovimiento.INTERESES_PAGADOS

/** Tipo de categoría financiera. */
enum class TipoCategoria { INGRESO, GASTO }

/** Tipos de cuenta soportados por Wallance. */
enum class TipoCuenta {
    EFECTIVO,
    BANCARIA,
    AHORROS,
    CORRIENTE,
    NEQUI,
    DAVIPLATA,
    BILLETERA_DIGITAL,
    TARJETA_CREDITO,
    INVERSION,
    OTRA
}

/** Dirección de un préstamo personal. */
enum class TipoPrestamo { OTORGADO, RECIBIDO }

/** Modalidad de interés aplicada a un préstamo. */
enum class TipoInteres {
    SIN_INTERES,
    PORCENTAJE_TOTAL,
    SIMPLE,
    COMPUESTO,
    PLANO
}

/** Periodicidad de una tasa de interés. */
enum class PeriodicidadTasa {
    DIARIA,
    SEMANAL,
    QUINCENAL,
    MENSUAL,
    BIMESTRAL,
    TRIMESTRAL,
    SEMESTRAL,
    ANUAL
}

/** Sistema de amortización del préstamo. */
enum class SistemaAmortizacion {
    PAGO_UNICO,
    FRANCES,
    ALEMAN,
    INTERESES_PERIODICOS,
    PERSONALIZADO
}

/** Frecuencia con la que se generan las cuotas. */
enum class FrecuenciaCuota {
    UNICA,
    SEMANAL,
    QUINCENAL,
    MENSUAL,
    BIMESTRAL,
    TRIMESTRAL,
    SEMESTRAL,
    ANUAL
}

/** Estado general de un préstamo. */
enum class EstadoPrestamo { ACTIVO, PAGADO, VENCIDO, CANCELADO, REESTRUCTURADO }

/** Estado de una cuota individual. */
enum class EstadoCuota { PENDIENTE, PAGADA, PARCIAL, VENCIDA }

/** Tipo de pago registrado sobre un préstamo. */
enum class TipoPago {
    ABONO,
    PAGO_CUOTA,
    EXTRAORDINARIO,
    INTERESES,
    MORA,
    AJUSTE,
    LIQUIDACION,
    NUEVO_DESEMBOLSO
}

/** Periodicidad de un presupuesto. */
enum class TipoPeriodoPresupuesto { SEMANAL, MENSUAL, ANUAL, PERSONALIZADO }

/** Estado de una suscripción recurrente. */
enum class EstadoSuscripcion { ACTIVA, PAUSADA, CANCELADA }
