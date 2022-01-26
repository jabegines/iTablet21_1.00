package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosHistorico
import es.albainformatica.albamobileandroid.DatosPrecios
import es.albainformatica.albamobileandroid.entity.HistoricoEnt


@Dao
interface HistoricoDao {

    @Query("SELECT * FROM Historico WHERE articuloId = :queArticulo AND clienteId = :queCliente")
    fun cargarHcoArtClte(queArticulo: Int, queCliente: Int): HistoricoEnt


    @Query("SELECT A.*, B.codigo, B.descripcion, C.piezas piezPedida, C.cantidad cantPedida, D.porcIva, " +
            " (E.ent - E.sal) stock, F.descripcion descrFto, G.texto FROM Historico A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " LEFT JOIN TmpHco C ON C.linea = A.historicoId " +
            " LEFT JOIN Ivas D ON D.tipo = B.tipoIva " +
            " LEFT JOIN Stock E ON E.articuloId = A.articuloId " +
            " LEFT JOIN Formatos F ON F.formatoId = A.formatoId " +
            " LEFT JOIN ArtHabituales G ON G.articuloId = A.articuloId AND G.formatoId = A.formatoId AND G.clienteId = :queCliente" +
            " WHERE A.clienteId = :queCliente AND B.descripcion LIKE(:queBuscar) " +
            " ORDER BY B.descripcion")
    fun abrirConBusqueda(queCliente: Int, queBuscar: String): List<DatosHistorico>


    @Query("SELECT A.*, B.codigo, B.descripcion, C.piezas piezPedida, C.cantidad cantPedida, D.porcIva, " +
            " (E.ent - E.sal) stock, F.descripcion descrFto, G.texto FROM Historico A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " LEFT JOIN TmpHco C ON C.linea = A.historicoId " +
            " LEFT JOIN Ivas D ON D.tipo = B.tipoIva " +
            " LEFT JOIN Stock E ON E.articuloId = A.articuloId " +
            " LEFT JOIN Formatos F ON F.formatoId = A.formatoId " +
            " LEFT JOIN ArtHabituales G ON G.articuloId = A.articuloId AND G.formatoId = A.formatoId AND G.clienteId = :queCliente" +
            " WHERE A.clienteId = :queCliente " +
            " ORDER BY B.descripcion")
    fun abrir(queCliente: Int): List<DatosHistorico>


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