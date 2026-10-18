package com.andrespar2.wallance.nucleo.basedatos.repositorios

import com.andrespar2.wallance.nucleo.basedatos.dao.ContactoDao
import com.andrespar2.wallance.nucleo.basedatos.entidades.Contacto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Error controlado al gestionar contactos financieros. */
class ErrorContacto(mensaje: String) : Exception(mensaje)

/**
 * Repositorio de contactos financieros.
 *
 * Los contactos son perfiles locales; no requieren cuenta de usuario ni
 * conexión a internet.
 */
@Singleton
class RepositorioContactos @Inject constructor(
    private val contactoDao: ContactoDao
) {

    fun todos(): Flow<List<Contacto>> = contactoDao.todos()

    fun buscar(busqueda: String): Flow<List<Contacto>> = contactoDao.buscar(busqueda)

    fun contacto(id: Long): Flow<Contacto?> = contactoDao.porIdFlujo(id)

    suspend fun porId(id: Long): Contacto? = contactoDao.porId(id)

    suspend fun todosSuspend(): List<Contacto> = contactoDao.todosSuspend()

    suspend fun contar(): Int = contactoDao.todosSuspend().size

    suspend fun guardar(contacto: Contacto): Long {
        if (contacto.nombre.isBlank()) throw ErrorContacto("El nombre del contacto es obligatorio.")
        return if (contacto.id == 0L) {
            contactoDao.insertar(contacto)
        } else {
            contactoDao.actualizar(contacto)
            contacto.id
        }
    }

    suspend fun eliminar(contacto: Contacto) {
        val prestamos = contactoDao.prestamosAsociados(contacto.id)
        if (prestamos > 0) {
            throw ErrorContacto(
                "Este contacto tiene $prestamos préstamo(s) asociados. " +
                    "Elimínalos o cancela las obligaciones antes de borrar el perfil."
            )
        }
        contactoDao.eliminar(contacto)
    }
}
