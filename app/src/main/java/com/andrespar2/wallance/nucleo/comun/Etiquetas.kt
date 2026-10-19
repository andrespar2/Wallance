package com.andrespar2.wallance.nucleo.comun

import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoSuscripcion
import com.andrespar2.wallance.nucleo.basedatos.entidades.FrecuenciaCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.PeriodicidadTasa
import com.andrespar2.wallance.nucleo.basedatos.entidades.SistemaAmortizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoInteres
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoMovimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPago
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPeriodoPresupuesto
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoRelacionContacto

/**
 * Etiquetas en español para los tipos del dominio financiero.
 *
 * Se centralizan aquí para que la interfaz nunca muestre los nombres técnicos
 * de los enums.
 */

val TipoMovimiento.etiqueta: String
    get() = when (this) {
        TipoMovimiento.INGRESO -> "Ingreso"
        TipoMovimiento.GASTO -> "Gasto"
        TipoMovimiento.TRANSFERENCIA -> "Transferencia"
        TipoMovimiento.AJUSTE -> "Ajuste"
        TipoMovimiento.PRESTAMO_OTORGADO -> "Préstamo otorgado"
        TipoMovimiento.PRESTAMO_RECIBIDO -> "Préstamo recibido"
        TipoMovimiento.COBRO_PRESTAMO -> "Cobro de préstamo"
        TipoMovimiento.PAGO_DEUDA -> "Pago de deuda"
        TipoMovimiento.INTERESES_COBRADOS -> "Intereses cobrados"
        TipoMovimiento.INTERESES_PAGADOS -> "Intereses pagados"
        TipoMovimiento.COMISION -> "Comisión"
        TipoMovimiento.REEMBOLSO -> "Reembolso"
    }

/** Tipos de movimiento que el usuario puede crear manualmente. */
val TipoMovimiento.esManual: Boolean
    get() = this == TipoMovimiento.INGRESO ||
        this == TipoMovimiento.GASTO ||
        this == TipoMovimiento.TRANSFERENCIA ||
        this == TipoMovimiento.AJUSTE

val TipoCuenta.etiqueta: String
    get() = when (this) {
        TipoCuenta.EFECTIVO -> "Efectivo"
        TipoCuenta.BANCARIA -> "Cuenta bancaria"
        TipoCuenta.AHORROS -> "Ahorros"
        TipoCuenta.CORRIENTE -> "Cuenta corriente"
        TipoCuenta.NEQUI -> "Nequi"
        TipoCuenta.DAVIPLATA -> "Daviplata"
        TipoCuenta.BILLETERA_DIGITAL -> "Billetera digital"
        TipoCuenta.TARJETA_CREDITO -> "Tarjeta de crédito"
        TipoCuenta.INVERSION -> "Inversión"
        TipoCuenta.OTRA -> "Otra"
    }

val TipoCategoria.etiqueta: String
    get() = when (this) {
        TipoCategoria.INGRESO -> "Ingreso"
        TipoCategoria.GASTO -> "Gasto"
    }

val TipoPrestamo.etiqueta: String
    get() = when (this) {
        TipoPrestamo.OTORGADO -> "Presté dinero"
        TipoPrestamo.RECIBIDO -> "Recibí un préstamo"
    }

val TipoRelacionContacto.etiqueta: String
    get() = when (this) {
        TipoRelacionContacto.PRESTAMISTA -> "Le presto dinero"
        TipoRelacionContacto.TOMADOR -> "Me presta dinero"
        TipoRelacionContacto.AMBOS -> "Ambas cosas"
    }

val TipoInteres.etiqueta: String
    get() = when (this) {
        TipoInteres.SIN_INTERES -> "Sin interés"
        TipoInteres.PORCENTAJE_TOTAL -> "Porcentaje sobre el total"
        TipoInteres.SIMPLE -> "Interés simple"
        TipoInteres.COMPUESTO -> "Interés compuesto"
        TipoInteres.PLANO -> "Interés plano"
    }

val PeriodicidadTasa.etiqueta: String
    get() = when (this) {
        PeriodicidadTasa.DIARIA -> "Diaria"
        PeriodicidadTasa.SEMANAL -> "Semanal"
        PeriodicidadTasa.QUINCENAL -> "Quincenal"
        PeriodicidadTasa.MENSUAL -> "Mensual"
        PeriodicidadTasa.BIMESTRAL -> "Bimestral"
        PeriodicidadTasa.TRIMESTRAL -> "Trimestral"
        PeriodicidadTasa.SEMESTRAL -> "Semestral"
        PeriodicidadTasa.ANUAL -> "Anual"
    }

val SistemaAmortizacion.etiqueta: String
    get() = when (this) {
        SistemaAmortizacion.PAGO_UNICO -> "Pago único"
        SistemaAmortizacion.FRANCES -> "Cuota fija (francés)"
        SistemaAmortizacion.ALEMAN -> "Abono fijo a capital (alemán)"
        SistemaAmortizacion.INTERESES_PERIODICOS -> "Solo intereses por periodo"
        SistemaAmortizacion.PERSONALIZADO -> "Cronograma personalizado"
    }

val FrecuenciaCuota.etiqueta: String
    get() = when (this) {
        FrecuenciaCuota.UNICA -> "Pago único"
        FrecuenciaCuota.SEMANAL -> "Semanal"
        FrecuenciaCuota.QUINCENAL -> "Quincenal"
        FrecuenciaCuota.MENSUAL -> "Mensual"
        FrecuenciaCuota.BIMESTRAL -> "Bimestral"
        FrecuenciaCuota.TRIMESTRAL -> "Trimestral"
        FrecuenciaCuota.SEMESTRAL -> "Semestral"
        FrecuenciaCuota.ANUAL -> "Anual"
    }

val EstadoPrestamo.etiqueta: String
    get() = when (this) {
        EstadoPrestamo.ACTIVO -> "Activo"
        EstadoPrestamo.PAGADO -> "Pagado"
        EstadoPrestamo.VENCIDO -> "Vencido"
        EstadoPrestamo.CANCELADO -> "Cancelado"
        EstadoPrestamo.REESTRUCTURADO -> "Reestructurado"
    }

val EstadoCuota.etiqueta: String
    get() = when (this) {
        EstadoCuota.PENDIENTE -> "Pendiente"
        EstadoCuota.PAGADA -> "Pagada"
        EstadoCuota.PARCIAL -> "Parcial"
        EstadoCuota.VENCIDA -> "Vencida"
    }

val TipoPago.etiqueta: String
    get() = when (this) {
        TipoPago.ABONO -> "Abono"
        TipoPago.PAGO_CUOTA -> "Pago de cuota"
        TipoPago.EXTRAORDINARIO -> "Pago extraordinario"
        TipoPago.INTERESES -> "Pago de intereses"
        TipoPago.MORA -> "Mora"
        TipoPago.AJUSTE -> "Ajuste"
        TipoPago.LIQUIDACION -> "Liquidación"
        TipoPago.NUEVO_DESEMBOLSO -> "Nuevo desembolso"
    }

val TipoPeriodoPresupuesto.etiqueta: String
    get() = when (this) {
        TipoPeriodoPresupuesto.SEMANAL -> "Semanal"
        TipoPeriodoPresupuesto.MENSUAL -> "Mensual"
        TipoPeriodoPresupuesto.ANUAL -> "Anual"
        TipoPeriodoPresupuesto.PERSONALIZADO -> "Personalizado"
    }

val EstadoSuscripcion.etiqueta: String
    get() = when (this) {
        EstadoSuscripcion.ACTIVA -> "Activa"
        EstadoSuscripcion.PAUSADA -> "Pausada"
        EstadoSuscripcion.CANCELADA -> "Cancelada"
    }

val TipoCategoria.soloIngreso: Boolean
    get() = this == TipoCategoria.INGRESO

val TipoPrestamo.esOtorgado: Boolean
    get() = this == TipoPrestamo.OTORGADO

val EstadoPrestamo.estaActivo: Boolean
    get() = this == EstadoPrestamo.ACTIVO || this == EstadoPrestamo.VENCIDO
