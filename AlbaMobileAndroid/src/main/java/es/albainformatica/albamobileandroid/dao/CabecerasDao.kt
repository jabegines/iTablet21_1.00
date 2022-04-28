package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.DatosCabFinDoc
import es.albainformatica.albamobileandroid.DatosResPedidos
import es.albainformatica.albamobileandroid.DatosVerDocs
import es.albainformatica.albamobileandroid.entity.CabecerasEnt


@Dao
interface CabecerasDao {

    @Query("SELECT cabeceraId FROM Cabeceras " +
            " WHERE tipoDoc = :queTipoDoc AND empresa = :queEmpresa AND almacen = :queAlmacen " +
            " AND serie = :queSerie AND numero = :queNumero AND ejercicio = :queEjercicio")
    fun getCabeceraId(queTipoDoc: Short, queEmpresa: Short, queAlmacen: Short, queSerie: String,
                        queNumero: Int, queEjercicio: Short): Int


    @Query("SELECT hojaReparto FROM Cabeceras WHERE hojaReparto IS NOT NULL AND hojaReparto > 0 LIMIT 1")
    fun buscarRutaActiva(): Int


    @Query("SELECT A.cabeceraId, A.tipoDoc, A.almacen, A.serie, A.numero, A.ejercicio, A.fecha, A.fechaEntrega, " +
            " A.observ1, A.observ2, B.codigo, B.nombre FROM Cabeceras A " +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " WHERE A.tipoDoc = 3")
    fun getResumenPedidos(): List<DatosResPedidos>


    @Query("SELECT observ1, observ2, dto, dto2, dto3, dto4, fPago FROM Cabeceras " +
            " WHERE cabeceraId = :queIdDoc")
    fun getDatosFinDoc(queIdDoc: Int): DatosCabFinDoc


    @Query("SELECT cabeceraId FROM Cabeceras WHERE estado = 'N' OR estado = 'R'")
    fun hayDocsParaEnviar(): Int

    @Query("SELECT cabeceraId FROM Cabeceras WHERE estado = 'N' OR estado = 'R' " +
            " OR ((firmado = 'T' OR tipoIncidencia > 0) AND estado <> 'X')")
    fun hayDocsParaEnvRep(): Int


    @Query("SELECT textoIncidencia FROM Cabeceras " +
            " WHERE cabeceraId = :queIdDoc")
    fun getTextoIncidencia(queIdDoc: Int): String

    @Query("SELECT numero FROM Cabeceras " +
            " WHERE tipoDoc = :queTipoDoc AND almacen = :queAlmacen AND serie =  :queSerie " +
            " AND numero = :queNumero AND ejercicio = :queEjercicio")
    fun getSerieNum(queTipoDoc: Short, queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): Int

    @Query("SELECT cabeceraId FROM Cabeceras WHERE estado = 'N' OR estado = 'R' OR estado = 'P'")
    fun getPdtesEnviar(): List<Int>


    @Query("UPDATE Cabeceras SET estado = 'N' WHERE cabeceraId = :queIdDoc")
    fun marcarParaEnviar(queIdDoc: Int)

    @Query("DELETE FROM Cabeceras WHERE estado <> 'N' and estado <> 'P'")
    fun borrarEnviadas()


    @Query("SELECT * FROM Cabeceras WHERE cabeceraId = :queIdDoc")
    fun cargarDoc(queIdDoc: Int): CabecerasEnt


    @Query("SELECT A.cabeceraId, A.tipoDoc, A.almacen, A.serie, A.numero, A.ejercicio, A.empresa, A.fecha, " +
            " A.clienteId, A.total, A.estado, A.facturado, B.nombre, B.nombreComercial, A.firmado, " +
            " A.imprimido, A.tipoIncidencia " +
            " FROM Cabeceras A " +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " WHERE A.clienteId = :queCliente AND A.empresa = :queEmpresa " +
            " AND CASE " +
            " WHEN :queFiltro = 1 THEN A.estado = 'P' " +
            " WHEN :queFiltro = 2 THEN (A.estado = 'N' OR A.estado = 'R') " +
            " WHEN :queFiltro = 3 THEN A.estado = 'X' " +
            " ELSE 1=1" +
            " END " +
            " ORDER BY substr(A.fecha, 7)||substr(A.fecha, 4, 2)||substr(A.fecha, 1, 2) DESC")
    fun abrirTodosClte(queCliente: Int, queEmpresa: Int, queFiltro: Int): MutableList<DatosVerDocs>


    @Query("SELECT A.cabeceraId, A.tipoDoc, A.almacen, A.serie, A.numero, A.ejercicio, A.empresa, A.fecha, " +
            " A.clienteId, A.total, A.estado, A.facturado, B.nombre, B.nombreComercial, A.firmado, " +
            " A.imprimido, A.tipoIncidencia " +
            " FROM Cabeceras A " +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " WHERE (A.estado = 'N' OR A.estado = 'R') " +
            " AND (julianday(substr(A.fecha, 7, 4) || '-' || substr(A.fecha, 4, 2) " +
            " || '-' || substr(A.fecha, 1, 2)) >= julianday(:desdeFecha))" +
            " AND (julianday(substr(A.fecha, 7, 4) || '-' || substr(A.fecha, 4, 2) " +
            " || '-' || substr(A.fecha, 1, 2)) <= julianday(:hastaFecha))" +
            " ORDER BY A.tipodoc, A.serie, A.numero")
    fun getInfDocumentos(desdeFecha: String, hastaFecha: String): List<DatosVerDocs>


    @Query("SELECT A.cabeceraId,  A.tipoDoc, A.almacen, A.serie, A.numero, A.ejercicio, A.empresa, A.fecha, " +
            " A.clienteId, A.total, A.estado, A.facturado, B.nombre, B.nombreComercial, A.firmado, " +
            " A.imprimido, A.tipoIncidencia " +
            " FROM Cabeceras A " +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " WHERE A.empresa = :queEmpresa " +
            " AND :cadFiltro " +
            " ORDER BY substr(A.fecha, 7)||substr(A.fecha, 4, 2)||substr(A.fecha, 1, 2) DESC")
    fun abrirTodos(queEmpresa: Int, cadFiltro: String): MutableList<DatosVerDocs>


    @Query("SELECT * FROM Cabeceras WHERE estado = 'N' or estado = 'R' " +
            "OR ((firmado = 'T' OR tipoIncidencia IS NOT NULL) AND estado <> 'X')")
    fun abrirParaEnvReparto(): MutableList<CabecerasEnt>

    @Query("SELECT * FROM Cabeceras WHERE estado = 'N' OR estado = 'R'")
    fun abrirParaEnviar(): MutableList<CabecerasEnt>

    @Query("SELECT * FROM Cabeceras WHERE numExport = :queNumExportacion")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<CabecerasEnt>


    @Query("UPDATE Cabeceras SET tipoIncidencia = :queTipoIncid, textoIncidencia = :queTexto " +
            " WHERE cabeceraId = :queIdDoc")
    fun setTextoIncidencia(queIdDoc: Int, queTipoIncid: Int, queTexto: String)

    @Query("UPDATE Cabeceras SET firmado = 'T', fechaFirma = :queFechaFirma, horaFirma = :queHoraFirma" +
            " WHERE cabeceraId = :queIdDoc")
    fun marcarComoEntregado(queIdDoc: Int, queFechaFirma: String, queHoraFirma: String)


    @Query("UPDATE Cabeceras SET imprimido = 'T' WHERE cabeceraId = :queIdDoc")
    fun marcarComoImprimido(queIdDoc: Int)

    @Query("UPDATE Cabeceras SET estado = 'X', numExport = :queNumExportacion " +
            " WHERE estado='N' OR estado='R'")
    fun marcarComoExportadas(queNumExportacion: Int)

    @Query("UPDATE Cabeceras SET estado = 'X', numExport = :queNumExportacion" +
            " WHERE estado='N' OR estado='R' OR ((firmado = 'T' OR tipoincidencia IS NOT NULL) AND estado <> 'X')")
     fun marcarComoExpReparto(queNumExportacion: Int)


     @Query("UPDATE Cabeceras SET estado = 'R' WHERE cabeceraId = :queIdDoc")
     fun reenviarDoc(queIdDoc: Int)


    @Query("UPDATE Cabeceras SET estado = 'N' WHERE numExport = -1")
    fun revertirEstado()

    @Query("UPDATE Cabeceras SET numExport = :queNumPaquete WHERE numExport = -1")
    fun actualizarNumPaquete(queNumPaquete: Int)


    @Query("DELETE FROM Cabeceras WHERE cabeceraId = :queIdDoc")
    fun borrarDoc(queIdDoc: Int)


    @Update
    fun actualizar(cabecera: CabecerasEnt)

    @Insert
    fun insertar(cabecera: CabecerasEnt): Long
}