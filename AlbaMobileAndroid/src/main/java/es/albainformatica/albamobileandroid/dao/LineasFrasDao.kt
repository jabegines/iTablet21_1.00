package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.DatosLinIva
import es.albainformatica.albamobileandroid.DatosLinVtas
import es.albainformatica.albamobileandroid.entity.LineasFrasEnt


@Dao
interface LineasFrasDao {


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



    @Query("SELECT A.codigoIva, A.importe, A.importeII, B.porcIva FROM LineasFras A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " WHERE A.facturaId = :queIdDoc")
    fun cargarDatosIva(queIdDoc: Int): List<DatosLinIva>


    @Query("DELETE FROM LineasFras WHERE facturaId = :queIdDoc AND flag3 = 128")
    fun borrarOftVolumen(queIdDoc: Int)


    @Query("DELETE FROM LineasFras WHERE lineaId = :queLineaId")
    fun borrarLinea(queLineaId: Int)


    @Update
    fun actualizar(linea: LineasFrasEnt)


    @Insert
    fun insertar(linea: LineasFrasEnt): Long
}