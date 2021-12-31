package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CostosArticulosEnt


@Dao
interface CostosArticulosDao {

    @Query("SELECT costo FROM CostosArticulos WHERE articuloId = :queArticulo AND empresa = :queEmpresa")
    fun getCostoArticulo(queArticulo: Int, queEmpresa: Int): Double



    @Query("DELETE FROM CostosArticulos")
    fun vaciar()

    @Insert
    fun insertar(costo: CostosArticulosEnt)
}