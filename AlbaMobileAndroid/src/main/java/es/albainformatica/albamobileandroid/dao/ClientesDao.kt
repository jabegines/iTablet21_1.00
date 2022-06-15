package es.albainformatica.albamobileandroid.dao

import androidx.room.*
import es.albainformatica.albamobileandroid.DatosReparto
import es.albainformatica.albamobileandroid.ListaClientes
import es.albainformatica.albamobileandroid.entity.ClientesEnt


@Dao
interface ClientesDao {

    @Query("SELECT DISTINCT B.cabeceraId, A.clienteId, B.tipoDoc, (B.serie || '/' || B.numero) AS serieNumero, " +
            " B.fecha, A.codigo, A.nombre, A.nombreComercial, IFNULL(C.clienteId, 0) tienePend, " +
            " B.estado, B.firmado, B.tipoIncidencia FROM Clientes A " +
            " LEFT JOIN Cabeceras B ON B.clienteId = A.clienteId " +
            " LEFT JOIN Pendiente C ON C.clienteId = A.clienteId AND C.estado <> 'L'AND C.tipodoc <> 2 " +
            " WHERE B.hojaReparto = :queRuta OR C.hoja = :queRuta" +
            " ORDER BY B.ordenReparto ")
    fun getDocsReparto(queRuta: Short): List<DatosReparto>


    @Query("UPDATE Clientes SET numExport = :queNumExportacion " +
            " WHERE numExport = -1")
    fun marcarNumExport(queNumExportacion: Int)

    @Query("UPDATE Clientes SET estado = 'XN', numExport = :queNumExportacion " +
            " WHERE estado = 'N'")
    fun marcarComoExportados(queNumExportacion: Int)


    @Query("UPDATE Clientes SET estado = 'XM', numExport = :queNumExportacion " +
            " WHERE estado = 'M'")
    fun marcarComoExpModif(queNumExportacion: Int)


    @Query("UPDATE Clientes SET pendiente = :queImporte WHERE clienteId = :queCliente")
    fun actualizarPendiente(queCliente: Int, queImporte: String)


    @Query("SELECT pendiente FROM Clientes WHERE clienteId = :queCliente")
    fun getPendienteClte(queCliente: Int): String


    @Query("SELECT clienteId, codigo, nombre, nombreComercial, flag FROM Clientes " +
            " ORDER BY CASE " +
            " WHEN :queOrdenacion = 0 THEN nombre " +
            " WHEN :queOrdenacion = 1 THEN nombreComercial " +
            " ELSE codigo " +
            " END")
    fun getCltes(queOrdenacion: Short): List<ListaClientes>

    @Query("SELECT clienteId, codigo, nombre, nombreComercial, flag FROM Clientes " +
            " WHERE nombre LIKE(:queBuscar) OR nombreComercial LIKE(:queBuscar) OR codigo LIKE(:queBuscar) " +
            " ORDER BY CASE " +
            " WHEN :queOrdenacion = 0 THEN nombre " +
            " WHEN :queOrdenacion = 1 THEN nombreComercial " +
            " ELSE codigo " +
            " END")
    fun getCltesBusq(queBuscar: String, queOrdenacion: Short): List<ListaClientes>


    @Query("SELECT * FROM Clientes WHERE estado = 'N' OR estado = 'M'")
    fun abrirParaEnviar(): List<ClientesEnt>


    @Query("SELECT * FROM Clientes WHERE numExport = :queNumExportacion")
    fun abrirParaEnvExp(queNumExportacion: Int): List<ClientesEnt>


    @Query("SELECT * FROM Clientes WHERE clienteId = :queCliente")
    fun abrirUnCliente(queCliente: Int): ClientesEnt


    @Query("SELECT clienteId FROM Clientes WHERE clienteId = :queCliente")
    fun existeClteId(queCliente: Int): Int


    @Query("SELECT clienteId FROM Clientes WHERE codigo = :queCodigo")
    fun existeCodigo(queCodigo: Int): Int


    @Query("SELECT * FROM Clientes")
    fun getAllCltes(): MutableList<ClientesEnt>


    @Query("DELETE FROM Clientes WHERE (estado IS NULL OR (estado<>'N' AND estado<>'M')) " +
            " AND clienteId NOT IN (SELECT clienteId FROM cabeceras WHERE estado = 'N' OR estado = 'P')")
    fun borrarViejos()


    @Query("DELETE FROM Clientes")
    fun vaciar()

    @Update
    fun actualizar(cliente: ClientesEnt)

    @Insert
    fun insertar(cliente: ClientesEnt)
}