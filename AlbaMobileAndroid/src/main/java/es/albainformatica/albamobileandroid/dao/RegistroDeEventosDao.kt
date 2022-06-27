package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
            " WHERE fecha = :queFecha AND empresa = :queEmpresa")
    fun getUltimoOrdenDiario(queFecha: String, queEmpresa: Short): Int


    @Query("SELECT * FROM RegistroDeEventos WHERE eventoId < :queId AND Empresa = :queEmpresa" +
            " ORDER BY eventoId DESC LIMIT 1")
    fun getEventoAnterior(queId: Int, queEmpresa: Short): RegistroDeEventosEnt

    @Query("UPDATE RegistroDeEventos SET referenciaAnterior = :queRefAnterior, huellaRefAnterior = :queHuellaAnterior, " +
            " huella = :queHuella, firma = :queFirma, firmaCadena = :queFirmaCadena, firmaVersion = :queFirmaVersion " +
            " WHERE eventoId = :queId")
    fun actualizarHuella(queId: Int, queRefAnterior: String, queHuellaAnterior: String, queHuella: String,
                         queFirma: String, queFirmaCadena: String, queFirmaVersion: String)

    @Insert
    fun insertar(evento: RegistroDeEventosEnt): Long
}