package com.andrespar2.wallance.nucleo.basedatos

import android.content.Context
import androidx.room.Room
import com.andrespar2.wallance.nucleo.basedatos.dao.CategoriaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.ContactoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.CotizacionDao
import com.andrespar2.wallance.nucleo.basedatos.dao.CuentaDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MetaAhorroDao
import com.andrespar2.wallance.nucleo.basedatos.dao.MovimientoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PrestamoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.PresupuestoDao
import com.andrespar2.wallance.nucleo.basedatos.dao.SuscripcionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias de la base de datos.
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloBaseDatos {

    @Provides
    @Singleton
    fun proporcionarBaseDatos(@ApplicationContext contexto: Context): BaseDatosWallance =
        Room.databaseBuilder(
            contexto,
            BaseDatosWallance::class.java,
            BaseDatosWallance.NOMBRE
        )
            .build()

    @Provides
    fun proporcionarCuentaDao(base: BaseDatosWallance): CuentaDao = base.cuentaDao()

    @Provides
    fun proporcionarCategoriaDao(base: BaseDatosWallance): CategoriaDao = base.categoriaDao()

    @Provides
    fun proporcionarMovimientoDao(base: BaseDatosWallance): MovimientoDao = base.movimientoDao()

    @Provides
    fun proporcionarContactoDao(base: BaseDatosWallance): ContactoDao = base.contactoDao()

    @Provides
    fun proporcionarPrestamoDao(base: BaseDatosWallance): PrestamoDao = base.prestamoDao()

    @Provides
    fun proporcionarPresupuestoDao(base: BaseDatosWallance): PresupuestoDao = base.presupuestoDao()

    @Provides
    fun proporcionarMetaAhorroDao(base: BaseDatosWallance): MetaAhorroDao = base.metaAhorroDao()

    @Provides
    fun proporcionarCotizacionDao(base: BaseDatosWallance): CotizacionDao = base.cotizacionDao()

    @Provides
    fun proporcionarSuscripcionDao(base: BaseDatosWallance): SuscripcionDao = base.suscripcionDao()
}
