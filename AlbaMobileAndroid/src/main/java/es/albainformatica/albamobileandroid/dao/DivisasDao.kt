package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosDivisa
import es.albainformatica.albamobileandroid.entity.DivisasEnt


@Dao
interface DivisasDao {

     @Query("SELECT descripcion FROM Divisas WHERE codigo = :queDivisa")
     fun getDescrDivisa(queDivisa: String): String


    @Query("SELECT * FROM Divisas ORDER BY orden")
    fun getAllDivisas(): MutableList<DivisasEnt>


    @Query("SELECT codigo, descripcion FROM Divisas ORDER BY orden")
    fun getDivParaSpinner(): MutableList<DatosDivisa>


    @Query("DELETE FROM Divisas")
    fun vaciar()

    @Insert
    fun insertar(divisa: DivisasEnt)

}