package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import es.albainformatica.albamobileandroid.entity.TmpHcoEnt


@Dao
interface TmpHcoDao {


    @Insert
    fun insertar(tempHco: TmpHcoEnt)
}