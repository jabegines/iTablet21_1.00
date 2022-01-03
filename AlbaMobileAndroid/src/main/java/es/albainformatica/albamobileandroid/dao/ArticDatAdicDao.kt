package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ArticDatAdicEnt


@Dao
interface ArticDatAdicDao {

    @Query("SELECT cadena FROM ArticDatAdic WHERE articuloId = :queArticulo")
    fun getDatosArticulo(queArticulo: Int): MutableList<String>


    @Query("DELETE FROM ArticDatAdic")
    fun vaciar()

    @Insert
    fun insertar(datAdic: ArticDatAdicEnt)
}