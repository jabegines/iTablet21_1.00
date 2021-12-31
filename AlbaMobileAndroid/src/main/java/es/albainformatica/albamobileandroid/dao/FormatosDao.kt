package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.FormatosEnt


@Dao
interface FormatosDao {


    @Query("DELETE FROM Formatos")
    fun vaciar()

    @Insert
    fun insert(formato: FormatosEnt)
}