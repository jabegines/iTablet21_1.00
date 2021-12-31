package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ClasificadoresEnt


@Dao
interface ClasificadoresDao {


    @Query("DELETE FROM Clasificadores")
    fun vaciar()

    @Insert
    fun insertar(clasificador: ClasificadoresEnt)
}