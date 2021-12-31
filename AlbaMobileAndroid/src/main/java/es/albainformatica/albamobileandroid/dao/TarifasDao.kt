package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.TarifasEnt


@Dao
interface TarifasDao {

    @Query("DELETE FROM Tarifas")
    fun vaciar()

    @Insert
    fun insertar(tarifa: TarifasEnt)
}