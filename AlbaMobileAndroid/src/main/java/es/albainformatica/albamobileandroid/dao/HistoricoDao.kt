package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosPrecios
import es.albainformatica.albamobileandroid.entity.HistoricoEnt


@Dao
interface HistoricoDao {

    @Query("SELECT articuloId FROM Historico " +
            " WHERE articuloId = :queArticulo AND clienteId = :queCliente")
    fun artEnHistorico(queCliente: Int, queArticulo: Int): Int


    @Query("SELECT precio, dto FROM Historico " +
            " WHERE clienteId = :queCliente AND articuloId = :queArticulo")
    fun getPrecio(queCliente: Int, queArticulo: Int): DatosPrecios

    @Query("SELECT precio, dto FROM Historico " +
            " WHERE clienteId = :queCliente AND articuloId = :queArticulo AND formatoId = :queFormato")
    fun getPrecioFto(queCliente: Int, queArticulo: Int, queFormato: Short): DatosPrecios

    @Query("DELETE FROM Historico")
    fun vaciar()

    @Insert
    fun insertar(historico: HistoricoEnt)
}