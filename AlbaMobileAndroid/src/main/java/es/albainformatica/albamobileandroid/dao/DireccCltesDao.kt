package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DireccCltesEnt


@Dao
interface DireccCltesDao {


    @Query("DELETE FROM DireccionesCltes")
    fun vaciar()

    @Insert
    fun insertar(direccClte: DireccCltesEnt)
}