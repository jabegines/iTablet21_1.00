package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CabecerasEnt


@Dao
interface CabecerasDao {

    @Query("SELECT cabeceraId FROM Cabeceras WHERE estado = 'N' OR estado = 'R' OR estado = 'P'")
    fun getPdtesEnviar(): List<Int>


    @Query("DELETE FROM Cabeceras WHERE estado <> 'N' and estado <> 'P'")
    fun borrarEnviadas()


    @Query("SELECT * FROM Cabeceras WHERE estado = 'N' or estado = 'R' " +
            "OR ((firmado = 'T' OR tipoIncidencia IS NOT NULL) AND estado <> 'X')")
    fun abrirParaEnvReparto(): MutableList<CabecerasEnt>

    @Query("SELECT * FROM Cabeceras WHERE estado = 'N' OR estado = 'R'")
    fun abrirParaEnviar(): MutableList<CabecerasEnt>

    @Query("SELECT * FROM Cabeceras WHERE numExport = :queNumExportacion")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<CabecerasEnt>


    @Query("UPDATE Cabeceras SET estado = 'X', numExport = :queNumExportacion " +
            " WHERE estado='N' OR estado='R'")
    fun marcarComoExportadas(queNumExportacion: Int)

    @Query("UPDATE Cabeceras SET estado = 'X', numExport = :queNumExportacion" +
            " WHERE estado='N' OR estado='R' OR ((firmado = 'T' OR tipoincidencia IS NOT NULL) AND estado <> 'X')")
     fun marcarComoExpReparto(queNumExportacion: Int)


    @Query("UPDATE Cabeceras SET estado = 'N' WHERE numExport = -1")
    fun revertirEstado()

    @Query("UPDATE Cabeceras SET numExport = :queNumPaquete WHERE numExport = -1")
    fun actualizarNumPaquete(queNumPaquete: Int)


    @Query("DELETE FROM Cabeceras WHERE cabeceraId = :queIdDoc")
    fun borrarDoc(queIdDoc: Int)

    @Insert
    fun insertar(cabecera: CabecerasEnt)
}