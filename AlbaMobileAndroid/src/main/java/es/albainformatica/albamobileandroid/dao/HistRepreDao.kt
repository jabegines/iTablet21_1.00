package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.HistRepreEnt


@Dao
interface HistRepreDao {

    @Query("SELECT * FROM HistRepre")
    fun getAllHco(): MutableList<HistRepreEnt>


    @Query("DELETE FROM HistRepre")
    fun vaciar()

    @Insert
    fun insertar(histRepre: HistRepreEnt)
}