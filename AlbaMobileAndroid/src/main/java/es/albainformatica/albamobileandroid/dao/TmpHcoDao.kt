package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.TmpHcoEnt


@Dao
interface TmpHcoDao {


    @Query("SELECT * FROM TmpHco")
    fun getAllLineas(): List<TmpHcoEnt>

    @Query("DELETE FROM TmpHco")
    fun vaciar()

    @Insert
    fun insertar(tempHco: TmpHcoEnt)
}