package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.NumExportEnt


@Dao
interface NumExportDao {

    @Query("SELECT * FROM NumExport ORDER BY numExport DESC")
    fun getAllExport(): MutableList<NumExportEnt>


    @Query("DELETE FROM NumExport WHERE numexport = :queNumPaquete")
    fun borrarExp(queNumPaquete: Int)


    @Insert
    fun insertar(numExport: NumExportEnt)
}