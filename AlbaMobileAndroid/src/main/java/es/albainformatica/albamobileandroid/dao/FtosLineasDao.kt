package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.FtosLineasEnt


@Dao
interface FtosLineasDao {

    @Insert
    fun insertar(ftoLinea: FtosLineasEnt)
}