package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt


@Dao
interface ContactosCltesDao {

    @Query("SELECT eMail FROM ContactosCltes WHERE clienteId = :queCliente")
    fun getEmailsClte(queCliente: Int): MutableList<String>


    @Query("SELECT * FROM ContactosCltes WHERE estado = 'N' OR estado = 'M'")
    fun getTlfsParaEnviar(): MutableList<ContactosCltesEnt>

    @Query("SELECT * FROM ContactosCltes WHERE numExport = :queNumExportacion")
    fun getTlfsParaEnvExp(queNumExportacion: Int): MutableList<ContactosCltesEnt>


    @Query("SELECT * FROM ContactosCltes WHERE clienteId = :queCliente ORDER BY orden")
    fun getTlfsCliente(queCliente: Int): MutableList<ContactosCltesEnt>


    @Query("DELETE FROM ContactosCltes")
    fun vaciar()

    @Insert
    fun insertar(contacto: ContactosCltesEnt)
}