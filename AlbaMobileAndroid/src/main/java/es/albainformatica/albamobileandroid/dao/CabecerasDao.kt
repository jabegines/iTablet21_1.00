package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.CabecerasEnt


@Dao
interface CabecerasDao {


    @Insert
    fun insertar(cabecera: CabecerasEnt)
}