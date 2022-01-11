package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.FormatosEnt


@Dao
interface FormatosDao {


    @Query("SELECT DISTINCT A.* FROM Formatos A " +
            " JOIN TrfFormatos B ON B.formatoId = A.formatoId AND B.articuloId = :queArticulo " +
            " ORDER BY A.formatoId")
    fun formatosALista(queArticulo: Int): List<FormatosEnt>


    @Query("DELETE FROM Formatos")
    fun vaciar()

    @Insert
    fun insert(formato: FormatosEnt)
}