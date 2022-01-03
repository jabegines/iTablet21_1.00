package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.HcoPorArticClteEnt


@Dao
interface HcoPorArticClteDao {


    @Query("DELETE FROM HcoPorArticClte")
    fun vaciar()

    @Insert
    fun insertar(hcoPorArticClte: HcoPorArticClteEnt)
}