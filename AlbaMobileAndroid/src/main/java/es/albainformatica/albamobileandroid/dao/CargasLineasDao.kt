package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosLinRecStock
import es.albainformatica.albamobileandroid.entity.CargasLineasEnt


@Dao
interface CargasLineasDao {

    @Query("SELECT A.articuloId, A.cajas, A.cantidad, A.lote, B.empresa FROM cargaslineas A " +
            " LEFT JOIN Cargas B ON B.cargaId = A.cargaId " +
            " WHERE B.estado = 'N'")
    fun getNoEnviadas(): List<DatosLinRecStock>

    @Query("DELETE FROM CargasLineas WHERE cargaLineaId IN " +
            " (SELECT cargaLineaId FROM CargasLineas A " +
            " LEFT JOIN Cargas B ON B.cargaId = A.cargaId " +
            " WHERE B.estado <> 'N')")
    fun borrarEnviadas()


    @Query("SELECT A.* FROM CargasLineas A" +
            " LEFT JOIN Cargas B ON B.cargaId = A.cargaId" +
            " WHERE B.estado = 'N' OR B.estado = 'R'")
    fun abrirParaEnviar(): MutableList<CargasLineasEnt>


    @Query("SELECT A.* FROM CargasLineas A" +
            " LEFT JOIN Cargas B ON B.cargaId = A.cargaId" +
            " WHERE B.numExport = :queNumExportacion")
    fun abrirExportacion(queNumExportacion: Int): MutableList<CargasLineasEnt>


    @Insert
    fun insertar(lineaCarga: CargasLineasEnt)
}