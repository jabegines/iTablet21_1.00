package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.OftVolumenEnt


@Dao
interface OftVolumenDao {


    @Query("DELETE FROM OftVolumen")
    fun vaciar()

    @Insert
    fun insertar(oftVolumen: OftVolumenEnt)
}