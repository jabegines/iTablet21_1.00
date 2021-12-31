package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.OftVolRangosEnt


@Dao
interface OftVolRangosDao {

    @Query("SELECT descuento FROM OftVolRangos WHERE idOferta = :queIdOfta" +
            " AND desdeImpte <= :queImporte AND (hastaImpte >= :queImporte OR hastaImpte = 0)")
    fun getDescuento(queIdOfta: Int, queImporte: Double): String


    @Query("DELETE FROM OftVolRangos")
    fun vaciar()

    @Insert
    fun insertar(oftVolRangos: OftVolRangosEnt)
}