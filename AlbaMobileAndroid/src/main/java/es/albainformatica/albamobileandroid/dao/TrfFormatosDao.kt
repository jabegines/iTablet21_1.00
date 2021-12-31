package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.TrfFormatosEnt


@Dao
interface TrfFormatosDao {


    @Query("DELETE FROM TrfFormatos")
    fun vaciar()

    @Insert
    fun insertar(trfFormato: TrfFormatosEnt)
}