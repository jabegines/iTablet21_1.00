package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DescuentosLinea
import es.albainformatica.albamobileandroid.entity.DtosLinFrasEnt
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt


@Dao
interface DtosLinFrasDao {

    @Query("SELECT A.* FROM DtosLinFras A " +
            " LEFT JOIN LineasFras C ON C.lineaId = A.lineaId " +
            " LEFT JOIN Facturas B ON B.facturaId = C.facturaId" +
            " WHERE B.estado = 'N' OR B.estado = 'R'")
    fun abrirParaEnviar(): MutableList<DtosLinFrasEnt>


    @Query("SELECT A.* FROM DtosLinFras A " +
            " LEFT JOIN LineasFras C ON C.lineaId = A.lineaId " +
            " LEFT JOIN Facturas B ON B.facturaId = C.facturaId" +
            " WHERE B.numExport = :queNumExportacion AND B.estadoInicial IS NULL")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<DtosLinFrasEnt>


    @Query("UPDATE DtosLinFras SET lineaId = :queLinea WHERE lineaId = -1")
    fun asignarLinea(queLinea: Int)


    @Query("SELECT lineaId FROM DtosLinFras WHERE lineaId = :queLinea")
    fun getLinea(queLinea: Int): Int


    @Query("SELECT * FROM DtosLinFras WHERE lineaId = :queLinea")
    fun getAllDtosLinea(queLinea: Int): List<DescuentosLinea>


    @Query("DELETE FROM DtosLinFras WHERE lineaId = :queLinea")
    fun borrarLinea(queLinea: Int)


    @Query("DELETE FROM DtosLinFras WHERE descuentoId = :queDto")
    fun borrarDto(queDto: Int)


    @Insert
    fun insertar(dtoLinea: DtosLinFrasEnt)
}