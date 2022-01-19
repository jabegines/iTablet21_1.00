package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.HistMesEnt


@Dao
interface HistMesDao {

    @Query("SELECT * FROM HistMes" +
            " WHERE clienteId = :queCliente AND articuloId = :queArticulo AND mes = :queMes")
    fun abrirClteArt(queCliente: Int, queArticulo: Int, queMes: Int): List<HistMesEnt>


    @Query("DELETE FROM HistMes")
    fun vaciar()

    @Insert
    fun insertar(hcoMes: HistMesEnt)
}