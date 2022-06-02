package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosPrecios
import es.albainformatica.albamobileandroid.DatosTrfArt
import es.albainformatica.albamobileandroid.entity.TarifasEnt


@Dao
interface TarifasDao {

    @Query("SELECT articuloId FROM Tarifas WHERE articuloId = :queArticulo AND tarifaId = :queTarifa")
    fun existe(queArticulo: Int, queTarifa: Short): Int


    @Query("SELECT A.articuloId, A.tarifaId, A.precio, A.dto, B.descrTarifa, '' descrFto FROM Tarifas A " +
            " LEFT JOIN CnfTarifas B ON B.codigo = A.tarifaId " +
            " WHERE A.articuloId = :queArticulo")
    fun getTarifasArt(queArticulo: Int): List<DatosTrfArt>


    @Query("SELECT precio, dto FROM Tarifas " +
            " WHERE articuloId= :queArticulo AND tarifaId = :queTarifa")
    fun getPrecioDto(queArticulo: Int, queTarifa: Short): DatosPrecios


    @Query("DELETE FROM Tarifas")
    fun vaciar()

    @Insert
    fun insertar(tarifa: TarifasEnt)
}