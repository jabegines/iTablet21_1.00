package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt


@Dao
interface ContactosCltesDao {

    @Query("UPDATE ContactosCltes SET numExport = :queNumExportacion " +
            " WHERE numExport = -1")
    fun marcarNumExport(queNumExportacion: Int)


    @Query("UPDATE ContactosCltes SET estado = 'XN', numExport = :queNumExportacion" +
            " WHERE estado = 'N' ")
    fun marcarComoExportados(queNumExportacion: Int)

    @Query("UPDATE ContactosCltes SET estado = 'XM', numExport = :queNumExportacion" +
            " WHERE estado = 'M' ")
    fun marcarComoExpModif(queNumExportacion: Int)


    @Query("SELECT MAX(orden) FROM ContactosCltes WHERE clienteId = :queCliente")
    fun getUltimoOrden(queCliente: Int): Int


    @Query("SELECT eMail FROM ContactosCltes WHERE clienteId = :queCliente")
    fun getEmailsClte(queCliente: Int): MutableList<String>


    @Query("SELECT * FROM ContactosCltes WHERE estado = 'N' OR estado = 'M'")
    fun getTlfsParaEnviar(): MutableList<ContactosCltesEnt>

    @Query("SELECT * FROM ContactosCltes WHERE numExport = :queNumExportacion")
    fun getTlfsParaEnvExp(queNumExportacion: Int): MutableList<ContactosCltesEnt>


    @Query("SELECT * FROM ContactosCltes WHERE clienteId = :queCliente ORDER BY orden")
    fun getTlfsCliente(queCliente: Int): MutableList<ContactosCltesEnt>


    @Query("DELETE FROM ContactosCltes WHERE contactoClteId = :queContactoId")
    fun borrar(queContactoId: Int)

    @Query("UPDATE ContactosCltes SET nombre = :queNombre, telefono1 = :queTlf1, telefono2 = :queTlf2, " +
            " obs1 = :queObs1, eMail = :queEmail, estado = :queEstado " +
            " WHERE contactoClteId = :queContactoId")
    fun actualizar(queContactoId: Int, queNombre: String, queTlf1: String, queTlf2: String,
                    queObs1: String, queEmail: String, queEstado: String)


    @Query("DELETE FROM ContactosCltes")
    fun vaciar()

    @Insert
    fun insertar(contacto: ContactosCltesEnt)
}