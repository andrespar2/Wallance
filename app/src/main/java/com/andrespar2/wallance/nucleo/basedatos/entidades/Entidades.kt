package com.andrespar2.wallance.nucleo.basedatos.entidades

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/** Cuenta financiera o billetera del usuario. */
@Entity(
    tableName = "cuentas",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class Cuenta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: TipoCuenta = TipoCuenta.OTRA,
    val icono: String = "cuenta",
    val color: Long = 0xFFFCD535,
    val moneda: String = "COP",
    val saldoInicial: BigDecimal = BigDecimal.ZERO,
    val activa: Boolean = true,
    val orden: Int = 0,
    val fechaCreacion: Long = System.currentTimeMillis()
)

/** Categoría de ingreso o gasto. */
@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nombre", "tipo"], unique = true)]
)
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: TipoCategoria,
    val icono: String = "categoria",
    val color: Long = 0xFFFCD535,
    val idPadre: Long? = null,
    val archivada: Boolean = false,
    val esPredeterminada: Boolean = false,
    val orden: Int = 0
)

/**
 * Movimiento financiero.
 *
 * El monto se guarda siempre como magnitud positiva en la moneda del
 * movimiento; el signo lo determina el tipo. Las cantidades monetarias se
 * almacenan como texto decimal para conservar precisión exacta.
 */
@Entity(
    tableName = "movimientos",
    indices = [
        Index("cuentaId"),
        Index("categoriaId"),
        Index("fecha"),
        Index("contactoId"),
        Index("prestamoId")
    ]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: TipoMovimiento,
    val monto: BigDecimal,
    val moneda: String = "COP",
    /** Tasa usada al registrar (moneda del movimiento -> moneda principal). */
    val tasaCambio: BigDecimal? = null,
    /** Equivalente en la moneda principal al momento de registrar. */
    val montoEnPrincipal: BigDecimal? = null,
    val categoriaId: Long? = null,
    val cuentaId: Long,
    /** Solo para transferencias: cuenta que recibe. */
    val cuentaDestinoId: Long? = null,
    /** Solo para AJUSTE: indica si el ajuste aumenta o disminuye el saldo. */
    val ajusteEsAumento: Boolean = true,
    val fecha: Long,
    val descripcion: String = "",
    val contactoId: Long? = null,
    /** Vínculo con el préstamo que originó el movimiento. */
    val prestamoId: Long? = null,
    val notas: String = "",
    /** Etiquetas separadas por coma. */
    val etiquetas: String = "",
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

/** Contacto financiero local (persona con la que se tienen operaciones). */
@Entity(tableName = "contactos")
data class Contacto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val apellido: String = "",
    val alias: String = "",
    val telefono: String = "",
    val correo: String = "",
    val fotoUri: String = "",
    val notas: String = "",
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    /** Nombre completo para mostrar. */
    val nombreCompleto: String
        get() = listOf(nombre, apellido).filter { it.isNotBlank() }
            .joinToString(" ").ifBlank { nombre }
}

/** Condiciones y estado de un préstamo personal. */
@Entity(
    tableName = "prestamos",
    indices = [Index("contactoId"), Index("estado")]
)
data class Prestamo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactoId: Long,
    val tipo: TipoPrestamo,
    val capital: BigDecimal,
    val moneda: String = "COP",
    val tasaTipo: TipoInteres = TipoInteres.SIN_INTERES,
    /** Valor de la tasa interpretado según [tasaTipo] y [tasaPeriodicidad]. */
    val tasaValor: BigDecimal = BigDecimal.ZERO,
    /** Indica si [tasaValor] es una tasa nominal (se divide entre los periodos del año). */
    val tasaEsNominal: Boolean = false,
    val tasaPeriodicidad: PeriodicidadTasa = PeriodicidadTasa.MENSUAL,
    val sistemaAmortizacion: SistemaAmortizacion = SistemaAmortizacion.PAGO_UNICO,
    val numeroCuotas: Int = 1,
    val frecuenciaCuotas: FrecuenciaCuota = FrecuenciaCuota.UNICA,
    val diaVencimiento: Int? = null,
    val fechaDesembolso: Long,
    val fechaVencimiento: Long,
    val moraHabilitada: Boolean = false,
    /** Interés moratorio diario en porcentaje; solo se aplica si está habilitado. */
    val moraTasa: BigDecimal = BigDecimal.ZERO,
    val comision: BigDecimal = BigDecimal.ZERO,
    val periodoGraciaDias: Int = 0,
    val notas: String = "",
    val garantia: String = "",
    val estado: EstadoPrestamo = EstadoPrestamo.ACTIVO,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

/** Cuota programada de un préstamo. */
@Entity(
    tableName = "cuotas",
    indices = [Index(value = ["prestamoId", "numero"], unique = true)]
)
data class Cuota(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prestamoId: Long,
    val numero: Int,
    val fechaVencimiento: Long,
    val valorTotal: BigDecimal,
    val capital: BigDecimal,
    val interes: BigDecimal,
    val pagado: BigDecimal = BigDecimal.ZERO,
    val estado: EstadoCuota = EstadoCuota.PENDIENTE,
    val fechaPago: Long? = null
)

/** Pago, abono o ajuste registrado sobre un préstamo. */
@Entity(
    tableName = "pagos",
    indices = [Index("prestamoId"), Index("cuotaId")]
)
data class Pago(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prestamoId: Long,
    val cuotaId: Long? = null,
    val fecha: Long,
    val monto: BigDecimal,
    val capital: BigDecimal = BigDecimal.ZERO,
    val interes: BigDecimal = BigDecimal.ZERO,
    val mora: BigDecimal = BigDecimal.ZERO,
    val tipo: TipoPago,
    val cuentaId: Long? = null,
    /** Movimiento financiero generado por este pago (evita doble contabilización). */
    val movimientoId: Long? = null,
    val nota: String = "",
    val referencia: String = ""
)

/** Presupuesto de gasto. */
@Entity(tableName = "presupuestos")
data class Presupuesto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val monto: BigDecimal,
    val moneda: String = "COP",
    val categoriaId: Long? = null,
    val cuentaId: Long? = null,
    val periodo: TipoPeriodoPresupuesto = TipoPeriodoPresupuesto.MENSUAL,
    val fechaInicio: Long,
    val fechaFin: Long? = null,
    /** Porcentaje (0-100) al que se muestra una alerta de consumo. */
    val alertaPorcentaje: Int = 80,
    val activo: Boolean = true
)

/** Meta de ahorro. */
@Entity(tableName = "metas_ahorro")
data class MetaAhorro(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val montoObjetivo: BigDecimal,
    val moneda: String = "COP",
    val fechaObjetivo: Long? = null,
    val cuentaId: Long? = null,
    val activa: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
)

/** Aporte realizado a una meta de ahorro. */
@Entity(
    tableName = "aportes",
    indices = [Index("metaId")]
)
data class Aporte(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metaId: Long,
    val monto: BigDecimal,
    val fecha: Long,
    val cuentaId: Long? = null,
    val movimientoId: Long? = null,
    val nota: String = ""
)

/** Suscripción o gasto recurrente. */
@Entity(tableName = "suscripciones")
data class Suscripcion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val costo: BigDecimal,
    val moneda: String = "COP",
    val frecuencia: FrecuenciaCuota = FrecuenciaCuota.MENSUAL,
    val proximoCobro: Long,
    val cuentaId: Long? = null,
    val categoriaId: Long? = null,
    val estado: EstadoSuscripcion = EstadoSuscripcion.ACTIVA,
    val notas: String = ""
)

/** Cotización de cambio entre dos monedas. */
@Entity(
    tableName = "cotizaciones",
    indices = [Index(value = ["base", "destino"], unique = true)]
)
data class Cotizacion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val base: String,
    val destino: String,
    /** Unidades de [destino] por una unidad de [base]. */
    val tasa: BigDecimal,
    val fechaConsulta: Long,
    val fuente: String = ""
)
