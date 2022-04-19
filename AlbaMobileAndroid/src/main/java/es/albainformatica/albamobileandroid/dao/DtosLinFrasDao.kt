package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DescuentosLinea
import es.albainformatica.albamobileandroid.entity.DtosLinFrasEnt



@Dao
interface DtosLinFrasDao {

    @Query("UPDATE DtosLinFras SET lineaId = :queLinea WHERE lineaId = -1")
    fun asignarLinea(queLinea: Int)


    @Query("SELECT lineaId FROM DtosLinFras WHERE lineaId = :queLinea")
    fun getLinea(queLinea: Int): Int


    @Query("SELECT * FROM DtosLinFras WHERE lineaId = :queLinea")
    fun getAllDtosLinea(queLinea: Int): List<DescuentosLinea>


    @Query("DELETE FROM DtosLinFras WHERE lineaId = :queLinea")
    fun borrarLinea(queLinea: Int)


    @Query("DELETE FROM DtosLinFras WHERE descuentoId = :queDto")
    fun borrarDto(queDto: Int)


    @Insert
    fun insertar(dtoLinea: DtosLinFrasEnt)
}