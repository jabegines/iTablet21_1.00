package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt


@Dao
interface DtosLineasDao {

    @Insert
    fun insertar(dtoLinea: DtosLineasEnt)
}