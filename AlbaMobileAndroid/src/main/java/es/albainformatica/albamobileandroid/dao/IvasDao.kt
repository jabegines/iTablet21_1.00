package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.IvasEnt


@Dao
interface IvasDao {


    @Query("DELETE FROM Ivas")
    fun vaciar()

    @Insert
    fun insertar(iva: IvasEnt)
}