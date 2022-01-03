package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CnfTarifasEnt


@Dao
interface CnfTarifasDao {

    @Query("DELETE FROM CnfTarifas")
    fun vaciar()

    @Insert
    fun insertar(cnfTarifa: CnfTarifasEnt)
}