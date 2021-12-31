package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.RuterosEnt


@Dao
interface RuterosDao {


    @Query("DELETE FROM Rutero")
    fun vaciar()

    @Insert
    fun insertar(rutero: RuterosEnt)

}