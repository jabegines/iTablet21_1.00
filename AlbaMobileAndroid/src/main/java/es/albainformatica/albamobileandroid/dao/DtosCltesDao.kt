package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DtosCltesEnt


@Dao
interface DtosCltesDao {

    @Query("SELECT dto FROM DtosCltes WHERE clienteId = :queCliente " +
            " ORDER BY idDescuento")
    fun getDtosClte(queCliente: Int): List<String>

    @Query("DELETE FROM DtosCltes")
    fun vaciar()

    @Insert
    fun insertar(dtoClte: DtosCltesEnt)
}