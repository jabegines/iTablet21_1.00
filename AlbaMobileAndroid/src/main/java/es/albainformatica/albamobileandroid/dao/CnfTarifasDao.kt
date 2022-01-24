package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CnfTarifasEnt


@Dao
interface CnfTarifasDao {

    @Query("SELECT flag FROM CnfTarifas WHERE codigo = :queTarifa")
    fun getFlag(queTarifa: Short): Int


    @Query("SELECT * FROM CnfTarifas " +
            " ORDER BY codigo")
    fun getAllCnfTarifas(): List<CnfTarifasEnt>


    @Query("SELECT codigo FROM CnfTarifas WHERE codigo = :queCodigo")
    fun existeCodigo(queCodigo: Short): Short

    @Query("DELETE FROM CnfTarifas")
    fun vaciar()

    @Insert
    fun insertar(cnfTarifa: CnfTarifasEnt)
}