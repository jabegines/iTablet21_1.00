package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ArtHabitualesEnt


@Dao
interface ArtHabitualesDao {

    @Query("SELECT articuloId FROM ArtHabituales")
    fun hayArtHabituales(): Int


    @Query("SELECT texto FROM ArtHabituales " +
            " WHERE articuloId = :queArticulo AND clienteId = :queCliente " +
            " AND (formatoId = :queFormato OR formatoId = 0)")
    fun getTexto(queArticulo: Int, queCliente: Int, queFormato: Short): String


    @Query("DELETE FROM ArtHabituales")
    fun vaciar()

    @Insert
    fun insertar(artHabitual: ArtHabitualesEnt)
}