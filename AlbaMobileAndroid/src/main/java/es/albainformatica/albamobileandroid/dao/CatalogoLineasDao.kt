package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CatalogoLineasEnt


@Dao
interface CatalogoLineasDao {

    @Query("SELECT * FROM CatalogoLineas WHERE articuloId = :queArticulo")
    fun getDatosArt(queArticulo: Int): MutableList<CatalogoLineasEnt>


    @Query("UPDATE CatalogoLineas SET articuloId = :queArticulo, cajas = :queCajas, piezas = :quePiezas, " +
            "cantidad = :queCantidad, precio = :quePrecio, precioII = :quePrecioII, importe = :queImporte, " +
            "importeII = :queImporteII, dto = :queDto WHERE linea = :queLinea")
    fun actualizarLinea(queArticulo: Int, queCajas: String, quePiezas: String, queCantidad: String, quePrecio: String,
                   quePrecioII: String, queImporte: String, queImporteII: String, queDto: String, queLinea: Int)


    @Query("UPDATE CatalogoLineas SET cajas = :queCajas, piezas = :quePiezas, cantidad = :queCantidad, " +
            " precio = :quePrecio, precioII = :quePrecioII, importe = :queImporte, importeII = :queImporteII," +
            " dto = :queDto WHERE articuloId = :queArticulo")
    fun actualizar(queCajas: String, quePiezas: String, queCantidad: String, quePrecio: String,
                   quePrecioII: String, queImporte: String, queImporteII: String, queDto: String, queArticulo: Int)


    @Query("SELECT linea FROM CatalogoLineas WHERE articuloId = :queArticulo")
    fun getLineaFromArt(queArticulo: Int): Int


    @Query("SELECT * FROM CatalogoLineas")
    fun getAllLineas(): MutableList<CatalogoLineasEnt>


    @Query("DELETE FROM CatalogoLineas")
    fun vaciar()

    @Insert
    fun insertar(catLinea: CatalogoLineasEnt)
}