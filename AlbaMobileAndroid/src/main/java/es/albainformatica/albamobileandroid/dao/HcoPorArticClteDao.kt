package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosHcArtClte
import es.albainformatica.albamobileandroid.entity.HcoPorArticClteEnt


@Dao
interface HcoPorArticClteDao {

    @Query("SELECT A.hcoPorcArticClteId, A.articuloId, B.codigo, B.descripcion, " +
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
    fun abrirHcoPorArtClte(queCliente: Int, queOrdenacion: Short): List<DatosHcArtClte>


    @Query("DELETE FROM HcoPorArticClte")
    fun vaciar()

    @Insert
    fun insertar(hcoPorArticClte: HcoPorArticClteEnt)
}