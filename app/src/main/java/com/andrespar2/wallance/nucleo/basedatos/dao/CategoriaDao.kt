package com.andrespar2.wallance.nucleo.basedatos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(categoria: Categoria): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarMuchas(categorias: List<Categoria>)

    @Update
    suspend fun actualizar(categoria: Categoria)

    @Delete
    suspend fun eliminar(categoria: Categoria)

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun porId(id: Long): Categoria?

    @Query("SELECT * FROM categorias ORDER BY orden ASC, nombre ASC")
    fun todas(): Flow<List<Categoria>>

    @Query(
        "SELECT * FROM categorias WHERE tipo = :tipo AND archivada = 0 " +
            "ORDER BY esPredeterminada DESC, orden ASC, nombre ASC"
    )
    fun activasPorTipo(tipo: TipoCategoria): Flow<List<Categoria>>

    @Query(
        "SELECT * FROM categorias WHERE tipo = :tipo AND archivada = 0 " +
            "ORDER BY esPredeterminada DESC, orden ASC, nombre ASC"
    )
    suspend fun activasPorTipoSuspend(tipo: TipoCategoria): List<Categoria>

    @Query("SELECT COUNT(*) FROM categorias")
    fun cantidad(): Flow<Int>

    @Query("SELECT COUNT(*) FROM movimientos WHERE categoriaId = :id")
    suspend fun movimientosAsociados(id: Long): Int
}
