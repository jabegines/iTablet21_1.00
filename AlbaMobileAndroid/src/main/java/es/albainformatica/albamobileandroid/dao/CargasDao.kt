package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.CargasEnt


@Dao
interface CargasDao {

    @Insert
    fun insertar(carga: CargasEnt)
}