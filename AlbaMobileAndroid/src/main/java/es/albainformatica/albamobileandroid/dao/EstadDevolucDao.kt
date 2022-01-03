package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.EstadDevolucEnt


@Dao
interface EstadDevolucDao {


    @Query("DELETE FROM EstadDevoluc")
    fun vaciar()

    @Insert
    fun insertar(estadDevoluc: EstadDevolucEnt)
}