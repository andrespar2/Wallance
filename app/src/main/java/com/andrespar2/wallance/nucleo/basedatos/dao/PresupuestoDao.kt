package com.andrespar2.wallance.nucleo.basedatos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.andrespar2.wallance.nucleo.basedatos.entidades.Aporte
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cotizacion
import com.andrespar2.wallance.nucleo.basedatos.entidades.MetaAhorro
import com.andrespar2.wallance.nucleo.basedatos.entidades.Presupuesto
import com.andrespar2.wallance.nucleo.basedatos.entidades.Suscripcion
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface PresupuestoDao {

    @Insert
    suspend fun insertar(presupuesto: Presupuesto): Long

    @Update
    suspend fun actualizar(presupuesto: Presupuesto)

    @Delete
    suspend fun eliminar(presupuesto: Presupuesto)

    @Query("SELECT * FROM presupuestos WHERE id = :id")
    suspend fun porId(id: Long): Presupuesto?

    @Query("SELECT * FROM presupuestos ORDER BY activo DESC, nombre ASC")
    fun todos(): Flow<List<Presupuesto>>

    @Query("SELECT * FROM presupuestos WHERE activo = 1")
    fun activos(): Flow<List<Presupuesto>>

    @Query("SELECT * FROM presupuestos WHERE activo = 1")
    suspend fun activosSuspend(): List<Presupuesto>
}

@Dao
interface MetaAhorroDao {

    @Insert
    suspend fun insertar(meta: MetaAhorro): Long

    @Update
    suspend fun actualizar(meta: MetaAhorro)

    @Delete
    suspend fun eliminar(meta: MetaAhorro)

    @Insert
    suspend fun insertarAporte(aporte: Aporte): Long

    @Query("DELETE FROM aportes WHERE id = :id")
    suspend fun eliminarAporte(id: Long)

    @Query("SELECT * FROM metas_ahorro WHERE id = :id")
    suspend fun metaPorId(id: Long): MetaAhorro?

    @Query("SELECT * FROM metas_ahorro WHERE id = :id")
    fun metaPorIdFlujo(id: Long): Flow<MetaAhorro?>

    @Query("SELECT * FROM metas_ahorro ORDER BY activa DESC, fechaCreacion DESC")
    fun todas(): Flow<List<MetaAhorro>>

    @Query("SELECT * FROM aportes WHERE metaId = :metaId ORDER BY fecha DESC")
    fun aportesDeMeta(metaId: Long): Flow<List<Aporte>>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM aportes WHERE metaId = :metaId")
    fun aportadoEnMeta(metaId: Long): Flow<BigDecimal>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM aportes WHERE metaId = :metaId")
    suspend fun aportadoEnMetaSuspend(metaId: Long): BigDecimal
}

@Dao
interface CotizacionDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun guardar(cotizacion: Cotizacion)

    @Query("SELECT * FROM cotizaciones WHERE base = :base AND destino = :destino LIMIT 1")
    suspend fun obtener(base: String, destino: String): Cotizacion?

    @Query("SELECT * FROM cotizaciones ORDER BY fechaConsulta DESC")
    fun todas(): Flow<List<Cotizacion>>
}

@Dao
interface SuscripcionDao {

    @Insert
    suspend fun insertar(suscripcion: Suscripcion): Long

    @Update
    suspend fun actualizar(suscripcion: Suscripcion)

    @Delete
    suspend fun eliminar(suscripcion: Suscripcion)

    @Query("SELECT * FROM suscripciones ORDER BY proximoCobro ASC")
    fun todas(): Flow<List<Suscripcion>>

    @Query("SELECT * FROM suscripciones WHERE estado = 'ACTIVA' ORDER BY proximoCobro ASC")
    fun activas(): Flow<List<Suscripcion>>

    @Query("SELECT * FROM suscripciones WHERE id = :id")
    suspend fun porId(id: Long): Suscripcion?
}
