package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosDetCarga
import es.albainformatica.albamobileandroid.DatosLinRecStock
import es.albainformatica.albamobileandroid.entity.CargasLineasEnt


@Dao
interface CargasLineasDao {

    @Query("SELECT A.cargaLineaId, A.articuloId, A.lote, A.cajas, A.cantidad, B.codigo, B.descripcion FROM CargasLineas A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId  " +
            " WHERE A.cargaLineaId = :queLinea")
    fun getDatosLinea(queLinea: Int): DatosDetCarga

    @Query("SELECT A.cargaLineaId, A.articuloId, A.lote, A.cajas, A.cantidad, B.codigo, B.descripcion FROM CargasLineas A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.cargaId = :queCarga")
    fun getCarga(queCarga: Int): List<DatosDetCarga>


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


    @Query("DELETE FROM CargasLineas WHERE cargaLineaId = :queLinea")
    fun borrarLinea(queLinea: Int)


    @Query("DELETE FROM CargasLineas WHERE cargaId = :queCarga")
    fun borrarCarga(queCarga: Int)


    @Insert
    fun insertar(lineaCarga: CargasLineasEnt)
}