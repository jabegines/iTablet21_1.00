package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.RatingArtEnt


@Dao
interface RatingArtDao {


    @Query("DELETE FROM RatingArt")
    fun vaciar()


    @Insert
    fun insertar(ratingArt: RatingArtEnt)
}