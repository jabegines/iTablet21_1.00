package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ArtHabitualesEnt


@Dao
interface ArtHabitualesDao {


    @Query("DELETE FROM ArtHabituales")
    fun vaciar()

    @Insert
    fun insertar(artHabitual: ArtHabitualesEnt)
}