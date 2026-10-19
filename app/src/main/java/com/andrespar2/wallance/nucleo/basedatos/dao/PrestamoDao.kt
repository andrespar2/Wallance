package com.andrespar2.wallance.nucleo.basedatos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.andrespar2.wallance.nucleo.basedatos.entidades.Cuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoCuota
import com.andrespar2.wallance.nucleo.basedatos.entidades.EstadoPrestamo
import com.andrespar2.wallance.nucleo.basedatos.entidades.Pago
import com.andrespar2.wallance.nucleo.basedatos.entidades.Prestamo
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

@Dao
interface PrestamoDao {

    // ------------------------------------------------------------------
    // Préstamos
    // ------------------------------------------------------------------

    @Insert
    suspend fun insertarPrestamo(prestamo: Prestamo): Long

    @Update
    suspend fun actualizarPrestamo(prestamo: Prestamo)

    @Delete
    suspend fun eliminarPrestamo(prestamo: Prestamo)

    @Query("SELECT * FROM prestamos WHERE id = :id")
    suspend fun prestamoPorId(id: Long): Prestamo?

    @Query("SELECT * FROM prestamos WHERE id = :id")
    fun prestamoPorIdFlujo(id: Long): Flow<Prestamo?>

    @Query("SELECT * FROM prestamos WHERE contactoId = :contactoId ORDER BY fechaCreacion DESC")
    fun prestamosDeContacto(contactoId: Long): Flow<List<Prestamo>>

    @Query("SELECT * FROM prestamos WHERE contactoId = :contactoId ORDER BY fechaCreacion DESC")
    suspend fun prestamosDeContactoSuspend(contactoId: Long): List<Prestamo>

    @Query("SELECT * FROM prestamos ORDER BY fechaCreacion DESC")
    fun todosLosPrestamos(): Flow<List<Prestamo>>

    @Query("SELECT * FROM prestamos ORDER BY fechaCreacion DESC")
    suspend fun todosLosPrestamosSuspend(): List<Prestamo>

    @Query("SELECT * FROM prestamos WHERE estado = :estado ORDER BY fechaVencimiento ASC")
    suspend fun prestamosPorEstado(estado: EstadoPrestamo): List<Prestamo>

    @Query("SELECT * FROM prestamos WHERE estado IN ('ACTIVO','VENCIDO')")
    fun prestamosActivos(): Flow<List<Prestamo>>

    @Query("SELECT COUNT(*) FROM prestamos")
    fun cantidadPrestamos(): Flow<Int>

    // ------------------------------------------------------------------
    // Cuotas
    // ------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCuotas(cuotas: List<Cuota>)

    @Insert
    suspend fun insertarCuota(cuota: Cuota): Long

    @Update
    suspend fun actualizarCuota(cuota: Cuota)

    @Query("SELECT * FROM cuotas WHERE prestamoId = :prestamoId ORDER BY numero ASC")
    fun cuotasDePrestamo(prestamoId: Long): Flow<List<Cuota>>

    @Query("SELECT * FROM cuotas WHERE prestamoId = :prestamoId ORDER BY numero ASC")
    suspend fun cuotasDePrestamoSuspend(prestamoId: Long): List<Cuota>

    @Query("SELECT * FROM cuotas WHERE id = :id")
    suspend fun cuotaPorId(id: Long): Cuota?

    @Query("DELETE FROM cuotas WHERE prestamoId = :prestamoId")
    suspend fun eliminarCuotasDePrestamo(prestamoId: Long)

    @Query(
        "SELECT * FROM cuotas WHERE estado != 'PAGADA' AND fechaVencimiento < :ahora " +
            "ORDER BY fechaVencimiento ASC"
    )
    suspend fun cuotasVencidas(ahora: Long): List<Cuota>

    // ------------------------------------------------------------------
    // Pagos
    // ------------------------------------------------------------------

    @Insert
    suspend fun insertarPago(pago: Pago): Long

    @Update
    suspend fun actualizarPago(pago: Pago)

    @Delete
    suspend fun eliminarPago(pago: Pago)

    @Query("SELECT * FROM pagos WHERE prestamoId = :prestamoId ORDER BY fecha DESC, id DESC")
    fun pagosDePrestamo(prestamoId: Long): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE prestamoId = :prestamoId ORDER BY fecha ASC")
    suspend fun pagosDePrestamoSuspend(prestamoId: Long): List<Pago>

    @Query("SELECT * FROM pagos WHERE id = :id")
    suspend fun pagoPorId(id: Long): Pago?

    @Query("DELETE FROM pagos WHERE prestamoId = :prestamoId")
    suspend fun eliminarPagosDePrestamo(prestamoId: Long)

    @Query("SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE prestamoId = :prestamoId")
    suspend fun totalPagado(prestamoId: Long): BigDecimal

    @Query("SELECT COALESCE(SUM(capital), 0) FROM pagos WHERE prestamoId = :prestamoId")
    suspend fun capitalPagado(prestamoId: Long): BigDecimal

    @Query("SELECT COALESCE(SUM(interes), 0) FROM pagos WHERE prestamoId = :prestamoId")
    suspend fun interesesPagados(prestamoId: Long): BigDecimal

    /**
     * Borra préstamo, cuotas y pagos de forma atómica.
     * Solo debe usarse cuando el usuario confirma la eliminación.
     */
    @Transaction
    suspend fun eliminarPrestamoCompleto(prestamo: Prestamo) {
        eliminarPagosDePrestamo(prestamo.id)
        eliminarCuotasDePrestamo(prestamo.id)
        eliminarPrestamo(prestamo)
    }

    /** Reemplaza el cronograma de cuotas de un préstamo de forma atómica. */
    @Transaction
    suspend fun reemplazarCuotas(prestamoId: Long, cuotas: List<Cuota>) {
        eliminarCuotasDePrestamo(prestamoId)
        insertarCuotas(cuotas)
    }
}
