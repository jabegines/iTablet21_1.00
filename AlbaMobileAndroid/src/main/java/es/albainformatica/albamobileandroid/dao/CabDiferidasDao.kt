package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CabDiferidasEnt


@Dao
interface CabDiferidasDao {

    @Query("SELECT cabDiferidaId FROM  CabDiferidas WHERE serie = :queSerie AND numero = :queNumero " +
            " AND ejercicio = :queEjercicio")
    fun getIdDocumento(queSerie: String, queNumero: Int, queEjercicio: Short): Int


    @Query("SELECT * FROM CabDiferidas WHERE cabDiferidaId = :queIdDocumento")
    fun getDatosDocumento(queIdDocumento: Int): CabDiferidasEnt


    @Query("DELETE FROM CabDiferidas")
    fun vaciar()

    @Insert
    fun insertar(cabDiferida: CabDiferidasEnt)
}