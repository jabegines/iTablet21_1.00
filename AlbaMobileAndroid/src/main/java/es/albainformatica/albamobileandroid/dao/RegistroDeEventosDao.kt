package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CargasEnt
import es.albainformatica.albamobileandroid.entity.RegistroDeEventosEnt

@Dao
interface RegistroDeEventosDao {

    @Query("UPDATE RegistroDeEventos SET estado = 'X', numExport = :queNumExportacion " +
            " WHERE estado = 'N' OR estado = 'R'")
    fun marcarComoExportados(queNumExportacion: Int)


    @Query("SELECT * FROM RegistroDeEventos WHERE numExport = :queNumExportacion")
    fun abrirExportacion(queNumExportacion: Int): MutableList<RegistroDeEventosEnt>


    @Query("SELECT * FROM RegistroDeEventos WHERE estado = 'N'")
    fun abrirParaEnviar(): MutableList<RegistroDeEventosEnt>



    @Query("SELECT MAX(ordenDiarioPuesto) FROM RegistroDeEventos " +
            " WHERE fecha = :queFecha")
    fun getUltimoOrdenDiario(queFecha: String): Int

    @Insert
    fun insertar(evento: RegistroDeEventosEnt)
}