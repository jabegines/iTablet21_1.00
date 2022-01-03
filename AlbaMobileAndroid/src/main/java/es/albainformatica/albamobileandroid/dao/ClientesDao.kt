package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.entity.ClientesEnt
import es.albainformatica.albamobileandroid.entity.TempCltesEnt


@Dao
interface ClientesDao {

    @Query("SELECT clienteId FROM Clientes WHERE clienteId = :queCliente")
    fun existeClteId(queCliente: Int): Int


    @Query("SELECT * FROM Clientes")
    fun getAllCltes(): MutableList<ClientesEnt>


    @Query("DELETE FROM Clientes WHERE (estado IS NULL OR (estado<>'N' AND estado<>'M')) " +
            " AND clienteId NOT IN (SELECT clienteId FROM cabeceras WHERE estado = 'N' OR estado = 'P')")
    fun borrarViejos()


    @Query("DELETE FROM Clientes")
    fun vaciar()

    @Update
    fun actualizar(cliente: ClientesEnt)

    @Insert
    fun insertar(cliente: ClientesEnt)
}