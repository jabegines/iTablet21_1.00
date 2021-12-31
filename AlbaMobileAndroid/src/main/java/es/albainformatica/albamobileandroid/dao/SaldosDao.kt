package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.SaldosEnt


@Dao
interface SaldosDao {

    @Query("UPDATE Saldos SET saldo = saldo + :queImporte WHERE clienteId = :queCliente AND empresa = :queEmpresa")
    fun actualizarSaldo(queCliente: Int, queEmpresa: Short, queImporte: Double)

    @Query("INSERT INTO Saldos (clienteId, empresa, saldo, facturasPtes, albaranesPtes, pedidosPtes)" +
            " VALUES (:queCliente, :queEmpresa, :queImporte, 0, 0, 0)")
    fun insertarSaldo(queCliente: Int, queEmpresa: Short, queImporte: Double)


    @Query("SELECT clienteId FROM Saldos WHERE clienteId = :queCliente")
    fun existeSaldo(queCliente: Int): Int


    @Query("SELECT Saldo FROM Saldos WHERE clienteId = :queCliente AND empresa = :queEmpresa")
    fun getSaldoClte(queCliente: Int, queEmpresa: Int): String


    @Query("SELECT Pendiente FROM Saldos WHERE clienteId = :queCliente AND empresa = :queEmpresa")
    fun getPendienteClte(queCliente: Int, queEmpresa: Int): String


    @Query("DELETE FROM Saldos")
    fun vaciar()

    @Insert
    fun insertar(saldo: SaldosEnt)
}