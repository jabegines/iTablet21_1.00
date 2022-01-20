package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DireccCltesEnt


@Dao
interface DireccCltesDao {

    @Query("UPDATE DireccionesCltes SET numExport = :queNumExportacion " +
            " WHERE numExport = -1")
    fun marcarNumExport(queNumExportacion: Int)


    @Query("UPDATE DireccionesCltes SET estado = 'XN', numExport = :queNumExportacion " +
            " WHERE estado = 'N'")
    fun marcarComoExportados(queNumExportacion: Int)


    @Query("UPDATE DireccionesCltes SET estado = 'XM', numExport = :queNumExportacion " +
            " WHERE estado = 'M'")
    fun marcarComoExpModif(queNumExportacion: Int)


    @Query("SELECT MAX(orden) FROM DireccionesCltes WHERE clienteId = :queCliente")
    fun getUltimoOrden(queCliente: Int): Int

    @Query("SELECT * FROM DireccionesCltes WHERE numExport = :queNumExportacion")
    fun getDirParaEnvExp(queNumExportacion: Int): List<DireccCltesEnt>


    @Query("SELECT * FROM DireccionesCltes WHERE estado = 'N' OR estado = 'M'")
    fun getDirParaEnv(): List<DireccCltesEnt>


    @Query("SELECT * FROM DireccionesCltes WHERE clienteId = :queCliente")
    fun getDirClte(queCliente: Int): List<DireccCltesEnt>


    @Query("SELECT * FROM DireccionesCltes WHERE clienteId = :queCliente " +
            " AND (direccionDoc = 'F' OR direccionDoc IS NULL)")
    fun getDirNoDocClte(queCliente: Int): List<DireccCltesEnt>


    @Query("DELETE FROM DireccionesCltes WHERE direccionId = :queDireccionId")
    fun borrar(queDireccionId: Int)


    @Query("UPDATE DireccionesCltes SET direccion = :queDireccion, localidad = :queLocalidad, " +
            " provincia = :queProvincia, cPostal = :queCPostal, estado = :queEstado " +
            " WHERE direccionId = :queDireccionId")
    fun actualizar(queDireccionId: Int, queDireccion: String, queLocalidad: String, queProvincia: String,
                   queCPostal: String, queEstado: String)


    @Query("DELETE FROM DireccionesCltes")
    fun vaciar()

    @Insert
    fun insertar(direccClte: DireccCltesEnt)
}