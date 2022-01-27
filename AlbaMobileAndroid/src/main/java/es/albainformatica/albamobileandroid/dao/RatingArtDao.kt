package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosPrecRat
import es.albainformatica.albamobileandroid.ListaPreciosEsp
import es.albainformatica.albamobileandroid.entity.RatingArtEnt


@Dao
interface RatingArtDao {

    @Query("SELECT A.ratingArtId, A.precio, A.dto, A.flag, B.descripcion, C.descripcion descrFto, " +
            " D.porcIva, E.precio prTarifa, E.dto dtoTarifa, F.precio prTrfFto, F.dto dtoTrfFto " +
            " FROM RatingArt A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " LEFT JOIN Ivas D ON D.tipo = B.tipoIva " +
            " LEFT JOIN Tarifas E ON E.articuloId = A.articuloId AND E.tarifaId = :queTarifaDoc " +
            " LEFT JOIN TrfFormatos F ON F.articuloId = A.articuloId AND F.formatoId = A.formatoId AND F.tarifaId = :queTarifaDoc " +
            " LEFT JOIN Clientes G ON G.clienteId = :queCliente " +
            " WHERE A.clienteId = :queCliente")
    fun getPreciosEspeciales(queTarifaDoc: Short, queCliente: Int): List<ListaPreciosEsp>


    @Query("SELECT A.ratingArtId, A.precio, A.dto, A.flag, B.descripcion, C.descripcion descrFto, " +
            " D.porcIva, E.precio prTarifa, E.dto dtoTarifa, F.precio prTrfFto, F.dto dtoTrfFto " +
            " FROM RatingArt A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " LEFT JOIN Ivas D ON D.tipo = B.tipoIva " +
            " LEFT JOIN Tarifas E ON E.articuloId = A.articuloId AND E.tarifaId = :queTarifaDoc " +
            " LEFT JOIN TrfFormatos F ON F.articuloId = A.articuloId AND F.formatoId = A.formatoId AND F.tarifaId = :queTarifaDoc " +
            " LEFT JOIN Clientes G ON G.clienteId = :queCliente " +
            " WHERE A.clienteId = :queCliente OR A.ramoId = G.ramo")
    fun getPreciosEspRamo(queTarifaDoc: Short, queCliente: Int): List<ListaPreciosEsp>


    @Query("SELECT precio, dto, flag FROM RatingArt " +
            " WHERE articuloId = :queArticulo AND almacen = :queAlmacen AND ramoId = :queRamo " +
            " AND julianday(inicio) <= julianday(:queFecha) " +
            " AND julianday(fin) >= julianday(:queFecha)")
    fun getPrecioRamo(queArticulo: Int, queAlmacen: Short, queRamo: Short, queFecha: String): DatosPrecRat


    @Query("SELECT precio, dto, flag FROM RatingArt " +
            " WHERE articuloId = :queArticulo AND almacen = :queAlmacen AND clienteId = :queCliente " +
            " AND julianday(inicio) <= julianday(:queFecha) " +
            " AND julianday(fin) >= julianday(:queFecha)")
    fun getPrecio(queArticulo: Int, queAlmacen: Short, queCliente: Int, queFecha: String): DatosPrecRat


    @Query("SELECT precio, dto, flag FROM RatingArt " +
            " WHERE articuloId = :queArticulo AND almacen = :queAlmacen AND clienteId = :queCliente " +
            " AND formatoId = :queFormato " +
            " AND julianday(inicio) <= julianday(:queFecha) " +
            " AND julianday(fin) >= julianday(:queFecha)")
    fun getPrecioFto(queArticulo: Int, queAlmacen: Short, queCliente: Int, queFormato: Short,
                     queFecha: String): DatosPrecRat


    @Query("DELETE FROM RatingArt")
    fun vaciar()


    @Insert
    fun insertar(ratingArt: RatingArtEnt)
}