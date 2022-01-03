package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.HistoricoEnt


@Dao
interface HistoricoDao {


    @Query("DELETE FROM Historico")
    fun vaciar()

    @Insert
    fun insertar(historico: HistoricoEnt)
}