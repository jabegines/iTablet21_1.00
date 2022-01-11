package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosPrecios
import es.albainformatica.albamobileandroid.entity.TarifasEnt


@Dao
interface TarifasDao {

    @Query("SELECT precio, dto FROM Tarifas " +
            " WHERE articuloId= :queArticulo AND tarifaId = :queTarifa")
    fun getPrecioDto(queArticulo: Int, queTarifa: Short): DatosPrecios


    @Query("DELETE FROM Tarifas")
    fun vaciar()

    @Insert
    fun insertar(tarifa: TarifasEnt)
}