package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosLinRecStock
import es.albainformatica.albamobileandroid.entity.LineasEnt


@Dao
interface LineasDao {

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


    @Insert
    fun insertar(linea: LineasEnt)
}