package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosPrecRat
import es.albainformatica.albamobileandroid.entity.RatingArtEnt


@Dao
interface RatingArtDao {

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