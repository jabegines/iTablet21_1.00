package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosHistMesAnyo
import es.albainformatica.albamobileandroid.DatosHistMesClte
import es.albainformatica.albamobileandroid.DatosHistMesDif
import es.albainformatica.albamobileandroid.TotalesHistMes
import es.albainformatica.albamobileandroid.entity.HistMesEnt


@Dao
interface HistMesDao {

    @Query("SELECT SUM(cantidad) sumCant, SUM(cantidadAnt) sumCantAnt, " +
            " SUM(importe) sumImpte, SUM(importeAnt) sumImpteAnt FROM HistMes " +
            " WHERE clienteId = :queCliente " +
            " GROUP BY clienteId")
    fun totalesHcoClte(queCliente: Int): TotalesHistMes


    @Query("SELECT A.histMesId, A.articuloId, B.codigo, B.descripcion, SUM(A.cantidad) sumCant, " +
            " SUM(A.cantidadAnt) sumCantAnt, SUM(A.importe) sumImpte, SUM(A.importeAnt) sumImpteAnt " +
            " FROM HistMes A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente " +
            " GROUP BY A.clienteId, A.articuloId " +
            " ORDER BY B.codigo")
    fun abrirHcoClte(queCliente: Int): List<DatosHistMesClte>


    @Query("SELECT A.histMesId, A.articuloId, B.codigo, B.descripcion, A.cantidadAnt, A.cantidad, " +
            " (A.cantidad - A.cantidadAnt) diferencia, A.mes FROM HistMes A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente AND A.mes = :queMes " +
            " ORDER BY B.codigo")
    fun abrir(queCliente: Int, queMes: Int): List<DatosHistMesDif>


    @Query("SELECT * FROM HistMes WHERE clienteId = :queCliente")
    fun abrirCliente(queCliente: Int): List<HistMesEnt>


    @Query("SELECT * FROM HistMes WHERE articuloId = :queArticulo AND clienteId = :queCliente")
    fun abrirArticulo(queArticulo: Int, queCliente: Int): List<HistMesEnt>


    @Query("SELECT A.articuloId, B.codigo, B.descripcion, A.cantidad, A.mes FROM HistMes A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente AND A.articuloId = :queArticulo " +
            " ORDER BY A.mes")
    fun abrirAnyo(queCliente: Int, queArticulo: Int): List<DatosHistMesAnyo>


    @Query("SELECT * FROM HistMes" +
            " WHERE clienteId = :queCliente AND articuloId = :queArticulo AND mes = :queMes")
    fun abrirClteArt(queCliente: Int, queArticulo: Int, queMes: Int): List<HistMesEnt>


    @Query("DELETE FROM HistMes")
    fun vaciar()

    @Insert
    fun insertar(hcoMes: HistMesEnt)
}