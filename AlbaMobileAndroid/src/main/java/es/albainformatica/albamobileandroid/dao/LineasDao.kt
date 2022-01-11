package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosLinRecStock
import es.albainformatica.albamobileandroid.DatosLinVtas
import es.albainformatica.albamobileandroid.entity.LineasEnt


@Dao
interface LineasDao {

    @Query("SELECT lineaId FROM Lineas WHERE cabeceraId = :queIdDoc AND flag3 = 128")
    fun hayOftVolumen(queIdDoc: Int): Int


    @Query("SELECT A.lineaId, A.articuloId, A.codArticulo, A.descripcion, A.tarifaId, A.codigoIva, A.cantidad, A.cajas, " +
            " A.lote, A.precio, A.importe, A.importeII, A.dto, A.tasa1, A.tasa2, A.flag, B.porcIva, " +
            " C.descripcion descrFto " +
            " FROM Lineas A " +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva " +
            " LEFT JOIN Formatos C ON C.formatoId = A.formatoId " +
            " WHERE A.cabeceraId = :queIdDoc")
    fun abrirLineas(queIdDoc: Int): List<DatosLinVtas>


    @Query("SELECT A.articuloId, A.cajas, A.cantidad, A.lote, B.empresa FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE (B.estado = 'N' OR B.estado = 'P') AND B.tipoDoc <> 3")
    fun getNoEnviadas(): List<DatosLinRecStock>


    @Query("DELETE FROM Lineas WHERE lineaId IN " +
            " (SELECT A.lineaId FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE B.estado <> 'N' AND B.estado <> 'P')")
    fun borrarEnviadas()


    @Query("SELECT A.* FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE B.numExport = :queNumExportacion AND B.estadoInicial IS NULL")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<LineasEnt>


    @Query("SELECT A.* FROM Lineas A " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = A.cabeceraId " +
            " WHERE B.estado = 'N' OR B.estado = 'R'")
    fun abrirParaEnviar(): MutableList<LineasEnt>


    @Query("DELETE FROM Lineas WHERE lineaId = :queLineaId")
    fun borrarLinea(queLineaId: Int)

    @Insert
    fun insertar(linea: LineasEnt): Long
}