package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.DatosLinIva
import es.albainformatica.albamobileandroid.DatosLinVtas
import es.albainformatica.albamobileandroid.DatosOftVol
import es.albainformatica.albamobileandroid.entity.LineasFrasEnt


@Dao
interface LineasFrasDao {

    @Query("SELECT A.* FROM LineasFras A " +
            " LEFT JOIN Facturas B ON B.facturaId = A.facturaId " +
            " WHERE B.estado = 'N' OR B.estado = 'R'")
    fun abrirParaEnviar(): MutableList<LineasFrasEnt>


    @Query("SELECT A.* FROM LineasFras A " +
            " LEFT JOIN Facturas B ON B.facturaId = A.facturaId " +
            " WHERE B.numExport = :queNumExportacion AND (B.estadoInicial IS NULL OR B.estadoInicial = '')")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<LineasFrasEnt>


    @Query("SELECT A.lineaId, A.facturaId AS cabeceraId, 0 AS tipoDoc, A.articuloId, A.codArticulo, A.descripcion, A.tarifaId, " +
            " A.codigoIva, A.cantidad, A.cantidadOrg, A.cajas, A.cajasOrg, A.piezas, A.piezasOrg, A.lote, A.precio, " +
            " A.precioII, A.precioTarifa, A.importe, A.importeII, A.dto, A.dtoImpte, A.dtoImpteII, A.dtoTarifa, " +
            " A.tasa1, A.tasa2, A.flag, A.flag3, A.flag5, A.tipoIncId, A.formatoId, A.textoLinea, A.modifNueva, " +
            " A.almacenPedido, A.dtoOftVol, A.ofertaId,  A.esEnlace, B.porcIva, C.descripcion descrFto " +
            " FROM LineasFras A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.lineaId = :queLineaId")
    fun getLinea(queLineaId: Int): DatosLinVtas


    @Query("SELECT A.lineaId, A.facturaId AS cabeceraId, 0 AS tipoDoc, A.articuloId, A.codArticulo, A.descripcion, " +
            " A.tarifaId, A.codigoIva, A.cantidad, A.cantidadOrg, A.cajas, A.cajasOrg, A.piezas, A.piezasOrg, A.lote, " +
            " A.precio, A.precioII, A.precioTarifa, A.importe, A.importeII, A.dto, A.dtoImpte, A.dtoImpteII, A.dtoTarifa, " +
            " A.tasa1, A.tasa2, A.flag, A.flag3, A.flag5, A.tipoIncId, A.formatoId, A.textoLinea, A.modifNueva, " +
            " A.almacenPedido, A.dtoOftVol, A.ofertaId, A.esEnlace, B.porcIva, C.descripcion descrFto " +
            " FROM LineasFras A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.facturaId = :queIdDoc")
    fun abrirLineas(queIdDoc: Int): List<DatosLinVtas>


    @Query("SELECT A.lineaId, A.facturaId AS cabeceraId, A.articuloId, A.codArticulo, A.descripcion, A.tarifaId, " +
            " A.codigoIva, A.cantidad, A.cantidadOrg, A.cajas, A.cajasOrg, A.piezas, A.piezasOrg, A.lote, A.precio, " +
            " A.precioII, A.precioTarifa, A.importe, A.importeII, A.dto, A.dtoImpte, A.dtoImpteII, A.dtoTarifa, " +
            " A.tasa1, A.tasa2, A.flag, A.flag3, A.flag5, A.tipoIncId, A.formatoId, A.textoLinea, A.modifNueva, " +
            " A.almacenPedido,  A.dtoOftVol, A.ofertaId, A.esEnlace, 0 AS porcIva, '' AS descrFto " +
            " FROM LineasFras A " +
            " WHERE A.facturaId <= 0")
    fun getLineasHuerfanas(): List<DatosLinVtas>


    @Query("SELECT lineaId FROM LineasFras WHERE facturaId = :queIdDoc AND flag3 = 128")
    fun hayOftVolumen(queIdDoc: Int): Int


    @Query("SELECT descripcion, importe FROM LineasFras WHERE flag3 = 128 AND facturaId = :queIdDoc")
    fun cargarOftVol(queIdDoc: Int): List<DatosOftVol>


    @Query("SELECT A.codigoIva, A.importe, A.importeII, B.porcIva FROM LineasFras A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " WHERE A.facturaId = :queIdDoc")
    fun cargarDatosIva(queIdDoc: Int): List<DatosLinIva>


    @Query("DELETE FROM LineasFras WHERE facturaId = :queIdDoc AND flag3 = 128")
    fun borrarOftVolumen(queIdDoc: Int)


    @Query("UPDATE LineasFras SET facturaId = :queIdDoc WHERE facturaId = -1")
    fun actualizarCabId(queIdDoc: Int)


    @Query("DELETE FROM LineasFras WHERE lineaId = :queLineaId")
    fun borrarLinea(queLineaId: Int)


    @Update
    fun actualizar(linea: LineasFrasEnt)


    @Insert
    fun insertar(linea: LineasFrasEnt): Long
}