package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.HistMesEnt


@Dao
interface HistMesDao {


    @Query("DELETE FROM HistMes")
    fun vaciar()

    @Insert
    fun insertar(hcoMes: HistMesEnt)
}