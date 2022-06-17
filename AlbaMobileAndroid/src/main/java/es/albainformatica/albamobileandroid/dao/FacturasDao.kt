package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.DatosVerDocs
import es.albainformatica.albamobileandroid.entity.FacturasEnt


@Dao
interface FacturasDao {

    @Query("SELECT A.facturaId AS cabeceraId, 1 AS tipoDoc, A.almacen, A.serie, A.numero, A.ejercicio, A.empresa, A.fecha, " +
            " A.clienteId, A.total, A.estado, 'F' AS facturado, B.nombre, B.nombreComercial, A.firmado, " +
            " A.imprimida AS imprimido, A.tipoIncidencia " +
            " FROM Facturas A " +
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

    @Query("SELECT A.facturaId AS cabeceraId, 1 AS tipoDoc, A.almacen, A.serie, A.numero, A.ejercicio, A.empresa, A.fecha, " +
            " A.clienteId, A.total, A.estado, 'F' AS facturado, B.nombre, B.nombreComercial, A.firmado, " +
            " A.imprimida AS imprimido, A.tipoIncidencia " +
            " FROM Facturas A " +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " WHERE A.empresa = :queEmpresa " +
            " AND :cadFiltro " +
            " ORDER BY substr(A.fecha, 7)||substr(A.fecha, 4, 2)||substr(A.fecha, 1, 2) DESC")
    fun abrirTodas(queEmpresa: Int, cadFiltro: String): MutableList<DatosVerDocs>


    @Query("UPDATE Facturas SET tipoIncidencia = :queTipoIncid, textoIncidencia = :queTexto " +
            " WHERE facturaId = :queIdDoc")
    fun setTextoIncidencia(queIdDoc: Int, queTipoIncid: Int, queTexto: String)


    @Query("SELECT * FROM Facturas WHERE estado = 'N' OR estado = 'R'")
    fun abrirParaEnviar(): MutableList<FacturasEnt>


    @Query("SELECT * FROM Facturas WHERE estado = 'N' or estado = 'R' " +
            "OR ((firmado = 'T' OR tipoIncidencia IS NOT NULL) AND estado <> 'X')")
    fun abrirParaEnvReparto(): MutableList<FacturasEnt>


    @Query("SELECT * FROM Facturas WHERE numExport = :queNumExportacion")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<FacturasEnt>


    @Query("UPDATE Facturas SET estado = 'X', numExport = :queNumExportacion" +
            " WHERE estado='N' OR estado='R' OR ((firmado = 'T' OR tipoincidencia IS NOT NULL) AND estado <> 'X')")
    fun marcarComoExpReparto(queNumExportacion: Int)


    @Query("UPDATE Facturas SET estado = 'X', numExport = :queNumExportacion " +
            " WHERE estado='N' OR estado='R'")
    fun marcarComoExportadas(queNumExportacion: Int)


    @Query("UPDATE Facturas SET imprimida = 'T' WHERE facturaId = :queIdDoc")
    fun marcarComoImprimida(queIdDoc: Int)


    @Query("UPDATE Facturas SET estado = 'R' WHERE facturaId = :queIdDoc")
    fun reenviarDoc(queIdDoc: Int)


    @Query("SELECT * FROM Facturas WHERE facturaId = :queIdDoc")
    fun cargarDoc(queIdDoc: Int): FacturasEnt


    @Query("SELECT numero FROM Facturas " +
            " WHERE almacen = :queAlmacen AND serie =  :queSerie " +
            " AND numero = :queNumero AND ejercicio = :queEjercicio")
    fun getSerieNum(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): Int



    @Update
    fun actualizar(factura: FacturasEnt)


    @Insert
    fun insertar(factura: FacturasEnt): Long
}