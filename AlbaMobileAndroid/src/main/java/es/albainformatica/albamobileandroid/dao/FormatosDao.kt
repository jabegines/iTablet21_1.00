package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosVtaFtos
import es.albainformatica.albamobileandroid.entity.FormatosEnt


@Dao
interface FormatosDao {

    @Query("SELECT DISTINCT A.formatoId, A.descripcion, A.dosis1, C.ftoLineaId, C.borrar, D.historicoId FROM Formatos A " +
            " JOIN TrfFormatos B ON B.formatoId = A.formatoId AND B.articuloId = :queArticulo " +
            " LEFT JOIN FtosLineas C ON C.articuloId = :queArticulo AND C.formatoId = A.formatoId " +
            " LEFT JOIN Historico D ON D.articuloId = :queArticulo AND D.clienteId = :queCliente AND D.formatoId = A.formatoId " +
            " ORDER BY A.formatoId")
    fun abrirFtosParaCat(queArticulo: Int, queCliente: Int): List<DatosVtaFtos>


    @Query("SELECT * FROM Formatos")
    fun getAllFormatos(): List<FormatosEnt>


    @Query("SELECT descripcion FROM Formatos WHERE formatoId = :queFormato")
    fun getDescripcion(queFormato: Int): String


    @Query("SELECT DISTINCT A.* FROM Formatos A " +
            " JOIN TrfFormatos B ON B.formatoId = A.formatoId AND B.articuloId = :queArticulo " +
            " ORDER BY A.formatoId")
    fun formatosALista(queArticulo: Int): List<FormatosEnt>


    @Query("DELETE FROM Formatos")
    fun vaciar()

    @Insert
    fun insert(formato: FormatosEnt)
}