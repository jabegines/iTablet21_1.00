package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.FtosLinFrasEnt


@Dao
interface FtosLinFrasDao {


    @Insert
    fun insertar(ftoLinea: FtosLinFrasEnt)
}