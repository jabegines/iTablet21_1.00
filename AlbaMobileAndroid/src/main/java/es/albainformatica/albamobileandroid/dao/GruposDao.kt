package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.GruposEnt


@Dao
interface GruposDao {


    @Query("DELETE FROM Grupos")
    fun vaciar()


    @Insert
    fun insertar(grupo: GruposEnt)
}