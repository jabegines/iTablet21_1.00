package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.FtosLineasEnt


@Dao
interface FtosLineasDao {

    @Query("SELECT ftoLineaId FROM FtosLineas " +
            "WHERE articuloId = :queArticulo AND borrar <> 'T'")
    fun artEnFtosLineas(queArticulo: Int): Int


    @Query("UPDATE FtosLineas SET cajas = '', piezas = '', cantidad = '', flag5 = 0, borrar = 'T' " +
            " WHERE articuloId = :queArticulo AND formatoId = :queFormato")
    fun inicializarFormato(queArticulo: Int, queFormato: Short)

    @Query("UPDATE FtosLineas SET cajas = :queCajas, piezas = :quePiezas, cantidad = :queCantidad, " +
            " precio = :quePrecio, dto = :queDto, textoLinea = :queTextoLinea, flag5 = :queFlag5, " +
            " borrar = :queBorrar WHERE articuloId = :queArticulo AND formatoId = :queFormato")
    fun actualizar(queCajas: String, quePiezas: String, queCantidad: String, quePrecio: String, queDto: String,
                queTextoLinea: String, queFlag5: Int, queBorrar: String, queArticulo: Int, queFormato: Short)

    @Query("SELECT * FROM FtosLineas WHERE articuloId = :queArticulo AND formatoId = :queFormato")
    fun getArtYFto(queArticulo: Int, queFormato: Short): List<FtosLineasEnt>

    @Query("SELECT * FROM FtosLineas")
    fun getAllFtos(): List<FtosLineasEnt>

    @Query("DELETE FROM FtosLineas")
    fun vaciar()

    @Insert
    fun insertar(ftoLinea: FtosLineasEnt)
}