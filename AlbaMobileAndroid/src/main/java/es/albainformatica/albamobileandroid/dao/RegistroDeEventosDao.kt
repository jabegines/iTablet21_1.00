package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.RegistroDeEventosEnt

@Dao
interface RegistroDeEventosDao {

    @Insert
    fun insertar(evento: RegistroDeEventosEnt)
}