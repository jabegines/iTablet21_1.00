package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ClientesEnt
import es.albainformatica.albamobileandroid.entity.TempCltesEnt


@Dao
interface TempCltesDao {


    @Query("SELECT * FROM TempCltes")
    fun getAllCltes(): List<ClientesEnt>


    @Query("DELETE FROM TempCltes")
    fun vaciar()

    @Insert
    fun insertar(tempClte: ClientesEnt)

}