package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.entity.LineasEnt


@Dao
interface LineasDao {

    @Query("SELECT A.codArticulo, A.descripcion, A.formatoId, A.cajas, A.cantidad, A.piezas, A.precio, A.dto, " +
            " A.importe, C.descripcion descrFto FROM Lineas A " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.cabeceraId = :queIdDoc")
    fun getResumenPedidos(queIdDoc: Int): List<DatosLinResPedidos>


    @Query("SELECT A.lineaId, A.precio, A.precioII, A.cantidad, A.dto, " +
            " B.tipoDoc, B.serie, B.numero, B.fecha, C.porcIva FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " LEFT JOIN Ivas C ON C.codigo = A.codigoIva " +
            " WHERE A.articuloId = :queArticulo AND B.clienteId = :queCliente " +
            " ORDER BY substr(B.fecha, 7)||substr(B.fecha, 4, 2)||substr(B.fecha, 1, 2) DESC")
    fun abrirHcoArtClte(queCliente: Int, queArticulo: Int): List<DatosHcoArtClte>


    @Query("UPDATE Lineas SET flag = 16384 WHERE lineaId = :queLinea")
    fun marcarComoPosOfta(queLinea: Int)

    @Query("SELECT * FROM Lineas WHERE articuloId = :queArticulo " +
            " AND flag & 32 = 0 " +
            " AND cabeceraId = :queIdDoc")
    fun getArticNoCambPr(queArticulo: Int, queIdDoc: Int): List<LineasEnt>


    @Query("UPDATE Lineas SET cabeceraId = :queIdDoc WHERE cabeceraId = -1")
    fun actualizarCabId(queIdDoc: Int)


    @Query("SELECT A.codigoIva, A.importe, A.importeII, B.porcIva FROM Lineas A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " WHERE A.cabeceraId = :queIdDoc")
    fun cargarDatosIva(queIdDoc: Int): List<DatosLinIva>

    @Query("SELECT descripcion, importe FROM Lineas WHERE flag3 = 128 AND cabeceraId = :queIdDoc")
    fun cargarOftVol(queIdDoc: Int): List<DatosOftVol>


    @Query("SELECT lineaId FROM Lineas WHERE cabeceraId = :queIdDoc AND flag3 = 128")
    fun hayOftVolumen(queIdDoc: Int): Int

    @Query("SELECT A.lineaId, A.cabeceraId, 0 AS tipoDoc, A.articuloId, A.codArticulo, A.descripcion, A.tarifaId, " +
            " A.codigoIva, A.cantidad, A.cantidadOrg, A.cajas, A.cajasOrg, A.piezas, A.piezasOrg, A.lote, A.precio, " +
            " A.precioII, A.precioTarifa, A.importe, A.importeII, A.dto, A.dtoImpte, A.dtoImpteII, A.dtoTarifa, " +
            " A.tasa1, A.tasa2, A.flag, A.flag3, A.flag5, A.tipoIncId, A.formatoId, A.textoLinea, A.modifNueva, " +
            " A.almacenPedido, A.dtoOftVol, A.ofertaId,  A.esEnlace, B.porcIva, C.descripcion descrFto " +
            " FROM Lineas A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.lineaId = :queLineaId")
    fun getLinea(queLineaId: Int): DatosLinVtas


    @Query("SELECT A.lineaId, A.cabeceraId, 0 AS tipoDoc, A.articuloId, A.codArticulo, A.descripcion, A.tarifaId, " +
            " A.codigoIva, A.cantidad, A.cantidadOrg, A.cajas, A.cajasOrg, A.piezas, A.piezasOrg, A.lote, A.precio, " +
            " A.precioII, A.precioTarifa, A.importe, A.importeII, A.dto, A.dtoImpte, A.dtoImpteII, A.dtoTarifa, " +
            " A.tasa1, A.tasa2, A.flag, A.flag3, A.flag5, A.tipoIncId, A.formatoId, A.textoLinea, A.modifNueva, " +
            " A.almacenPedido, A.dtoOftVol, A.ofertaId, A.esEnlace, B.porcIva, C.descripcion descrFto " +
            " FROM Lineas A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.cabeceraId = :queIdDoc")
    fun abrirLineas(queIdDoc: Int): List<DatosLinVtas>


    @Query("SELECT A.articuloId, A.cajas, A.cantidad, A.lote, B.empresa FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE (B.estado = 'N' OR B.estado = 'P') AND B.tipoDoc <> 3")
    fun getNoEnviadas(): List<DatosLinRecStock>


    @Query("SELECT A.lineaId, A.cabeceraId, A.articuloId, A.codArticulo, A.descripcion, A.tarifaId, " +
            " A.codigoIva, A.cantidad, A.cantidadOrg, A.cajas, A.cajasOrg, A.piezas, A.piezasOrg, A.lote, A.precio, " +
            " A.precioII, A.precioTarifa, A.importe, A.importeII, A.dto, A.dtoImpte, A.dtoImpteII, A.dtoTarifa, " +
            " A.tasa1, A.tasa2, A.flag, A.flag3, A.flag5, A.tipoIncId, A.formatoId, A.textoLinea, A.modifNueva, " +
            " A.almacenPedido,  A.dtoOftVol, A.ofertaId, A.esEnlace, 0 AS porcIva, '' AS descrFto " +
            " FROM Lineas A " +
            " WHERE A.cabeceraId <= 0")
    fun getLineasHuerfanas(): List<DatosLinVtas>


    @Query("DELETE FROM Lineas WHERE lineaId IN " +
            " (SELECT A.lineaId FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE B.estado <> 'N' AND B.estado <> 'P')")
    fun borrarEnviadas()

    @Query("DELETE FROM Lineas WHERE cabeceraId = :queIdDoc AND flag3 = 128")
    fun borrarOftVolumen(queIdDoc: Int)


    @Query("SELECT A.* FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE B.numExport = :queNumExportacion AND B.estadoInicial = ''")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<LineasEnt>


    @Query("SELECT A.* FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE B.estado = 'N' OR B.estado = 'R'")
    fun abrirParaEnviar(): MutableList<LineasEnt>


    @Query("DELETE FROM Lineas WHERE lineaId = :queLineaId")
    fun borrarLinea(queLineaId: Int)


    @Query("UPDATE Lineas SET cajas = :queCajas, piezas = :quePiezas, cantidad = :queCantidad, " +
            " importe = :queImporte, importeII = :queImpteII WHERE lineaId = :queLineaId")
    fun actDatosReparto(queLineaId: Int, queCajas: String, quePiezas: String, queCantidad: String,
                        queImporte: String, queImpteII: String)


    @Update
    fun actualizar(linea: LineasEnt)

    @Insert
    fun insertar(linea: LineasEnt): Long
}