package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.Semillas
import com.andrespar2.wallance.nucleo.basedatos.dao.CategoriaDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Error controlado al gestionar categorías. */
class ErrorCategoria(mensaje: String) : Exception(mensaje)

/** Repositorio de categorías con siembra inicial del catálogo predeterminado. */
@Singleton
class RepositorioCategorias @Inject constructor(
    private val categoriaDao: CategoriaDao
) {

    fun todas(): Flow<List<Categoria>> = categoriaDao.todas()

    fun activasPorTipo(tipo: TipoCategoria): Flow<List<Categoria>> =
        categoriaDao.activasPorTipo(tipo)

    suspend fun porId(id: Long): Categoria? = categoriaDao.porId(id)

    suspend fun contar(): Int = categoriaDao.activasPorTipoSuspend(TipoCategoria.GASTO).size +
        categoriaDao.activasPorTipoSuspend(TipoCategoria.INGRESO).size

    /** Inserta el catálogo predeterminado solo la primera vez. */
    suspend fun siembraInicial() {
        if (contar() == 0) {
            categoriaDao.insertarMuchas(Semillas.categoriasIniciales())
        }
    }

    suspend fun guardar(categoria: Categoria) {
        if (categoria.nombre.isBlank()) throw ErrorCategoria("El nombre de la categoría es obligatorio.")
        val duplicada = categoriaDao.activasPorTipoSuspend(categoria.tipo)
            .any { it.nombre.equals(categoria.nombre, ignoreCase = true) && it.id != categoria.id }
        if (duplicada) throw ErrorCategoria("Ya existe una categoría con ese nombre.")
        if (categoria.id == 0L) {
            categoriaDao.insertar(categoria)
        } else {
            categoriaDao.actualizar(categoria)
        }
    }

    /**
     * Archiva una categoría. Los movimientos asociados se conservan íntegros.
     */
    suspend fun archivar(categoria: Categoria) {
        categoriaDao.actualizar(categoria.copy(archivada = true))
    }

    suspend fun restaurar(categoria: Categoria) {
        categoriaDao.actualizar(categoria.copy(archivada = false))
    }

    suspend fun movimientosAsociados(id: Long): Int = categoriaDao.movimientosAsociados(id)
}
