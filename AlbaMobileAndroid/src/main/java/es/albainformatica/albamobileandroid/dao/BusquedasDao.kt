package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.BusquedasEnt


@Dao
interface BusquedasDao {


    @Query("DELETE FROM Busquedas")
    fun vaciar()

    @Insert
    fun insertar(busqueda: BusquedasEnt)
}