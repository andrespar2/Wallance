package com.andrespar2.wallance.nucleo.basedatos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuenta
import kotlinx.coroutines.flow.Flow

@Dao
interface CuentaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(cuenta: Cuenta): Long

    @Update
    suspend fun actualizar(cuenta: Cuenta)

    @Delete
    suspend fun eliminar(cuenta: Cuenta)

    @Query("SELECT * FROM cuentas WHERE id = :id")
    suspend fun porId(id: Long): Cuenta?

    @Query("SELECT * FROM cuentas WHERE id = :id")
    fun porIdFlujo(id: Long): Flow<Cuenta?>

    @Query("SELECT * FROM cuentas ORDER BY orden ASC, nombre ASC")
    fun todas(): Flow<List<Cuenta>>

    @Query("SELECT * FROM cuentas WHERE activa = 1 ORDER BY orden ASC, nombre ASC")
    fun activas(): Flow<List<Cuenta>>

    @Query("SELECT * FROM cuentas ORDER BY orden ASC, nombre ASC")
    suspend fun todasSuspend(): List<Cuenta>

    @Query("SELECT * FROM cuentas WHERE nombre = :nombre LIMIT 1")
    suspend fun porNombre(nombre: String): Cuenta?

    @Query("SELECT COUNT(*) FROM cuentas")
    fun cantidad(): Flow<Int>
}
