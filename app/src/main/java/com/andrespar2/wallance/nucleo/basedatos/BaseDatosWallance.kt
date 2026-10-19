package com.andrespar2.wallance.nucleo.basedatos

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andrespar2.wallance.nucleo.basedatos.dao.CategoriaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.ContactoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.CotizacionDao
import com.andrespar2.wallance.nucleo.basedatos.dao.CuentaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MetaAhorroDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PrestamoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PresupuestoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.SuscripcionDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Aporte
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cotizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.MetaAhorro
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import com.andrespar2.wallance.nucleo.basedatos.entidades.Pago
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.Presupuesto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Suscripcion

/**
 * Base de datos principal de Wallance.
 *
 * Diseño offline-first: toda la información financiera vive en el dispositivo.
 * Los saldos siempre pueden reconstruirse a partir de los movimientos.
 */
@Database(
    entities = [
        Cuenta::class,
        Categoria::class,
        Movimiento::class,
        Contacto::class,
        Prestamo::class,
        Cuota::class,
        Pago::class,
        Presupuesto::class,
        MetaAhorro::class,
        Aporte::class,
        Suscripcion::class,
        Cotizacion::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(ConvertidoresRoom::class)
abstract class BaseDatosWallance : RoomDatabase() {
    abstract fun cuentaDao(): CuentaDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun contactoDao(): ContactoDao
    abstract fun prestamoDao(): PrestamoDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun metaAhorroDao(): MetaAhorroDao
    abstract fun cotizacionDao(): CotizacionDao
    abstract fun suscripcionDao(): SuscripcionDao

    companion object {
        const val NOMBRE = "wallance.db"
    }
}
