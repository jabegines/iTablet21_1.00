package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ArticulosEnt


@Dao
interface ArticulosDao {


    @Query("DELETE FROM Articulos")
    fun vaciar()

    @Insert
    fun insertar(articulo: ArticulosEnt)
}