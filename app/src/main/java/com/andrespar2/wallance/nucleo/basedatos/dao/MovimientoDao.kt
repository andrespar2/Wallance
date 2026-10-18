package com.andrespar2.wallance.nucleo.basedatos.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.andrespar2.wallance.nucleo.basedatos.entidades.Movimiento
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insertar(movimiento: Movimiento): Long

    @Update
    suspend fun actualizar(movimiento: Movimiento)

    @Query("DELETE FROM movimientos WHERE id = :id")
    suspend fun eliminarPorId(id: Long): Int

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun porId(id: Long): Movimiento?

    @Query("SELECT * FROM movimientos WHERE id = :id")
    fun porIdFlujo(id: Long): Flow<Movimiento?>

    @Query(
        "SELECT * FROM movimientos " +
            "WHERE fecha BETWEEN :inicio AND :fin " +
            "ORDER BY fecha DESC, id DESC"
    )
    fun entreFechas(inicio: Long, fin: Long): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun todos(): Flow<List<Movimiento>>

    @Query(
        "SELECT * FROM movimientos WHERE prestamoId = :prestamoId ORDER BY fecha DESC, id DESC"
    )
    fun porPrestamo(prestamoId: Long): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos WHERE prestamoId = :prestamoId ORDER BY fecha ASC")
    suspend fun porPrestamoSuspend(prestamoId: Long): List<Movimiento>

    @Query("SELECT COUNT(*) FROM movimientos")
    fun cantidad(): Flow<Int>

    /** Suma de ingresos puros (excluye préstamos y transferencias). */
    @Query(
        "SELECT COALESCE(SUM(montoEnPrincipal), 0) FROM movimientos " +
            "WHERE tipo IN ('INGRESO','REEMBOLSO') AND fecha BETWEEN :inicio AND :fin"
    )
    fun ingresosEntre(inicio: Long, fin: Long): Flow<BigDecimal>

    /** Suma de gastos puros (excluye pagos de deuda y transferencias). */
    @Query(
        "SELECT COALESCE(SUM(montoEnPrincipal), 0) FROM movimientos " +
            "WHERE tipo IN ('GASTO','COMISION') AND fecha BETWEEN :inicio AND :fin"
    )
    fun gastosEntre(inicio: Long, fin: Long): Flow<BigDecimal>

    /** Suma de intereses cobrados en el periodo. */
    @Query(
        "SELECT COALESCE(SUM(montoEnPrincipal), 0) FROM movimientos " +
            "WHERE tipo = 'INTERESES_COBRADOS' AND fecha BETWEEN :inicio AND :fin"
    )
    fun interesesCobradosEntre(inicio: Long, fin: Long): Flow<BigDecimal>

    @Query(
        "SELECT COALESCE(SUM(montoEnPrincipal), 0) FROM movimientos " +
            "WHERE tipo = 'INTERESES_PAGADOS' AND fecha BETWEEN :inicio AND :fin"
    )
    fun interesesPagadosEntre(inicio: Long, fin: Long): Flow<BigDecimal>

    /** Gasto acumulado de una categoría en el periodo. */
    @Query(
        "SELECT COALESCE(SUM(montoEnPrincipal), 0) FROM movimientos " +
            "WHERE categoriaId = :categoriaId AND tipo = 'GASTO' " +
            "AND fecha BETWEEN :inicio AND :fin"
    )
    suspend fun gastoEnCategoria(categoriaId: Long, inicio: Long, fin: Long): BigDecimal

    /** Gasto acumulado de una cuenta en el periodo. */
    @Query(
        "SELECT COALESCE(SUM(montoEnPrincipal), 0) FROM movimientos " +
            "WHERE cuentaId = :cuentaId AND tipo = 'GASTO' " +
            "AND fecha BETWEEN :inicio AND :fin"
    )
    suspend fun gastoEnCuenta(cuentaId: Long, inicio: Long, fin: Long): BigDecimal

    /** Movimientos de una cuenta en un periodo. */
    @Query(
        "SELECT * FROM movimientos " +
            "WHERE (cuentaId = :cuentaId OR cuentaDestinoId = :cuentaId) " +
            "AND fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC"
    )
    fun deCuenta(cuentaId: Long, inicio: Long, fin: Long): Flow<List<Movimiento>>

    @Query(
        "SELECT * FROM movimientos WHERE descripcion LIKE '%' || :busqueda || '%' " +
            "OR notas LIKE '%' || :busqueda || '%' OR etiquetas LIKE '%' || :busqueda || '%' " +
            "ORDER BY fecha DESC"
    )
    fun buscar(busqueda: String): Flow<List<Movimiento>>

    /**
     * Reconstruye el saldo de una cuenta a partir de sus movimientos.
     * Las transferencias se contabilizan correctamente en ambas cuentas.
     */
    @Query(
        "SELECT COALESCE(SUM(CASE " +
            "WHEN tipo IN ('INGRESO','REEMBOLSO','COBRO_PRESTAMO','INTERESES_COBRADOS') " +
            "THEN montoEnPrincipal " +
            "WHEN tipo = 'AJUSTE' AND ajusteEsAumento = 1 THEN montoEnPrincipal " +
            "WHEN tipo = 'TRANSFERENCIA' AND cuentaDestinoId = :cuentaId " +
            "THEN montoEnPrincipal ELSE 0 END), 0) " +
            "- COALESCE(SUM(CASE " +
            "WHEN tipo IN ('GASTO','COMISION','PAGO_DEUDA','INTERESES_PAGADOS') " +
            "THEN montoEnPrincipal " +
            "WHEN tipo = 'AJUSTE' AND ajusteEsAumento = 0 THEN montoEnPrincipal " +
            "WHEN tipo = 'TRANSFERENCIA' AND cuentaId = :cuentaId " +
            "AND (cuentaDestinoId IS NULL OR cuentaDestinoId != :cuentaId) " +
            "THEN montoEnPrincipal ELSE 0 END), 0) " +
            "FROM movimientos WHERE cuentaId = :cuentaId OR cuentaDestinoId = :cuentaId"
    )
    suspend fun balanceMovimientosCuenta(cuentaId: Long): BigDecimal

    @Query(
        "SELECT COALESCE(SUM(CASE " +
            "WHEN tipo = 'INGRESO' OR tipo = 'REEMBOLSO' OR tipo = 'COBRO_PRESTAMO' " +
            "OR tipo = 'INTERESES_COBRADOS' THEN montoEnPrincipal " +
            "WHEN tipo = 'GASTO' OR tipo = 'COMISION' OR tipo = 'PAGO_DEUDA' " +
            "OR tipo = 'INTERESES_PAGADOS' THEN -montoEnPrincipal " +
            "ELSE 0 END), 0) FROM movimientos"
    )
    suspend fun balanceGlobal(): BigDecimal
}
