package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosPrecios
import es.albainformatica.albamobileandroid.DatosTrfArt
import es.albainformatica.albamobileandroid.entity.TrfFormatosEnt


@Dao
interface TrfFormatosDao {

    @Query("SELECT A.articuloId, A.tarifaId, A.precio, A.dto, B.descrTarifa, C.descripcion descrFto " +
            " FROM TrfFormatos A " +
            " LEFT JOIN CnfTarifas B ON B.codigo = A.tarifaId " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.articuloId = :queArticulo")
    fun getTarifasArt(queArticulo: Int): List<DatosTrfArt>


    @Query("SELECT precio, dto FROM TrfFormatos " +
            " WHERE articuloId= :queArticulo AND tarifaId = :queTarifa AND formatoId = :queFormato")
    fun getPrecioDto(queArticulo: Int, queTarifa: Short, queFormato: Short): DatosPrecios


    @Query("DELETE FROM TrfFormatos")
    fun vaciar()

    @Insert
    fun insertar(trfFormato: TrfFormatosEnt)
}