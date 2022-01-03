package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.LineasEnt


@Dao
interface LineasDao {


    @Insert
    fun insertar(linea: LineasEnt)
}