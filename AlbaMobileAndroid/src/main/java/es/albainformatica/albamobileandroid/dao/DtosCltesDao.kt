package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DtosCltesEnt


@Dao
interface DtosCltesDao {


    @Query("DELETE FROM DtosCltes")
    fun vaciar()

    @Insert
    fun insertar(dtoClte: DtosCltesEnt)
}