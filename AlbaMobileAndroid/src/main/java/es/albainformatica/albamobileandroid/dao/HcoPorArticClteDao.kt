package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosArtHcArtClte
import es.albainformatica.albamobileandroid.DatosDocsHcArtClte
import es.albainformatica.albamobileandroid.entity.HcoPorArticClteEnt


@Dao
interface HcoPorArticClteDao {

    @Query("SELECT A.hcoPorArticClteId, A.articuloId, B.codigo, B.descripcion, " +
            " C.cantidad cantPedida, D.porcDevol FROM HcoPorArticClte A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " LEFT JOIN TmpHco C ON C.linea = A.hcoPorArticClteId " +
            " LEFT JOIN estadDevoluc D ON D.clienteId = :queCliente AND D.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente " +
            " GROUP BY A.articuloId " +
            " ORDER BY CASE " +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirArtsHcoPorArtClte(queCliente: Int, queOrdenacion: Short): List<DatosArtHcArtClte>


    @Query("SELECT hcoPorArticClteId, articuloId, tipoDoc, serie, numero, fecha, ventas, devoluciones FROM HcoPorArticClte" +
            " WHERE articuloId = :queArticulo AND clienteId = :queCliente " +
            " ORDER BY substr(fecha, 7)||substr(fecha, 4, 2)||substr(fecha, 1, 2) DESC")
    fun abrirDocsHcoPorArtClte(queArticulo: Int, queCliente: Int): List<DatosDocsHcArtClte>


    @Query("DELETE FROM HcoPorArticClte")
    fun vaciar()

    @Insert
    fun insertar(hcoPorArticClte: HcoPorArticClteEnt)
}