package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.HcoCompSemMesEnt


@Dao
interface HcoCompSemMesDao {


    @Query("DELETE FROM HcoCompSemMes")
    fun vaciar()

    @Insert
    fun insertar(hcoCompSemMes: HcoCompSemMesEnt)
}