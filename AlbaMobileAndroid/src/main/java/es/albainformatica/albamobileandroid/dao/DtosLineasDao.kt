package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt


@Dao
interface DtosLineasDao {

    @Query("SELECT A.* FROM DtosLineas A " +
            " LEFT JOIN Lineas C ON C.lineaId = A.lineaId " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = C.cabeceraId" +
            " WHERE B.numExport = :queNumExportacion AND B.estadoInicial IS NULL")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<DtosLineasEnt>

    @Query("SELECT A.* FROM DtosLineas A " +
            " LEFT JOIN Lineas C ON C.lineaId = A.lineaId " +
            " LEFT JOIN Cabeceras B ON B.cabeceraId = C.cabeceraId" +
            " WHERE B.estado = 'N' OR B.estado = 'R'")
    fun abrirParaEnviar(): MutableList<DtosLineasEnt>


    @Query("UPDATE DtosLineas SET lineaId = :queLinea WHERE lineaId = -1")
    fun asignarLinea(queLinea: Int)


    @Query("DELETE FROM DtosLineas WHERE lineaId = :queLinea")
    fun borrarLinea(queLinea: Int)


    @Insert
    fun insertar(dtoLinea: DtosLineasEnt)
}