package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CargasEnt


@Dao
interface CargasDao {

    @Query("SELECT cargaId FROM Cargas WHERE estado = 'N' OR estado = 'R'")
    fun getPdtesEnviar(): List<Int>


    @Query("DELETE FROM Cargas WHERE estado <> 'N'")
    fun borrarEnviadas()


    @Query("UPDATE Cargas SET estado = 'N' WHERE numExport = -1")
    fun revertirEstado()


    @Query("UPDATE Cargas SET estado = 'X', numExport = :queNumExportacion " +
            " WHERE estado = 'N' OR estado = 'R'")
    fun marcarComoExportadas(queNumExportacion: Int)


    @Query("SELECT * FROM Cargas WHERE numExport = :queNumExportacion")
    fun abrirExportacion(queNumExportacion: Int): MutableList<CargasEnt>

    @Query("SELECT * FROM Cargas WHERE estado = 'N' OR estado = 'R'")
    fun abrirParaEnviar(): MutableList<CargasEnt>

    @Insert
    fun insertar(carga: CargasEnt)
}