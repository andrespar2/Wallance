package com.andrespar2.wallance.nucleo.basedatos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {

    @Insert
    suspend fun insertar(contacto: Contacto): Long

    @Update
    suspend fun actualizar(contacto: Contacto)

    @Delete
    suspend fun eliminar(contacto: Contacto)

    @Query("SELECT * FROM contactos WHERE id = :id")
    suspend fun porId(id: Long): Contacto?

    @Query("SELECT * FROM contactos WHERE id = :id")
    fun porIdFlujo(id: Long): Flow<Contacto?>

    @Query("SELECT * FROM contactos ORDER BY nombre ASC")
    fun todos(): Flow<List<Contacto>>

    @Query("SELECT * FROM contactos ORDER BY nombre ASC")
    suspend fun todosSuspend(): List<Contacto>

    @Query(
        "SELECT * FROM contactos WHERE nombre LIKE '%' || :busqueda || '%' " +
            "OR apellido LIKE '%' || :busqueda || '%' OR alias LIKE '%' || :busqueda || '%' " +
            "ORDER BY nombre ASC"
    )
    fun buscar(busqueda: String): Flow<List<Contacto>>

    @Query("SELECT COUNT(*) FROM contactos")
    fun cantidad(): Flow<Int>

    @Query("SELECT COUNT(*) FROM prestamos WHERE contactoId = :id")
    suspend fun prestamosAsociados(id: Long): Int
}
