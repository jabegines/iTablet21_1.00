package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.LineasDifEnt


@Dao
interface LineasDifDao {

    @Query("SELECT * FROM LineasDiferidas WHERE cabeceraId = :queIdDocumento")
    fun getLineasDoc(queIdDocumento: Int): MutableList<LineasDifEnt>


    @Query("DELETE FROM LineasDiferidas")
    fun vaciar()

    @Insert
    fun insertar(lineaDif: LineasDifEnt)
}