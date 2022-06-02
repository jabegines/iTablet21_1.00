package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ArticClasifEnt


@Dao
interface ArticClasifDao {

    @Query("SELECT articuloId FROM ArticClasif WHERE articuloId = :queArticuloId AND clasificadorId = :queClasificador")
    fun existe(queArticuloId: Int, queClasificador: Int): Int


    @Query("DELETE FROM ArticClasif")
    fun vaciar()

    @Insert
    fun insertar(articClasif: ArticClasifEnt)
}