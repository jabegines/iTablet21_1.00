package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.CargasLineasEnt


@Dao
interface CargasLineasDao {

    @Insert
    fun insertar(lineaCarga: CargasLineasEnt)
}