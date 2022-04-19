package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DescuentosLinea
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt


@Dao
interface DtosLineasDao {

    @Query("SELECT * FROM DtosLineas WHERE lineaId = :queLinea")
    fun getAllDtosLinea(queLinea: Int): List<DescuentosLinea>


    @Query("SELECT lineaId FROM DtosLineas WHERE lineaId = :queLinea")
    fun getLinea(queLinea: Int): Int


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

    @Query("DELETE FROM DtosLineas WHERE descuentoId = :queDto")
    fun borrarDto(queDto: Int)

    @Query("UPDATE DtosLineas SET descuento = :queDescuento, importe = :queImporte, cantidad1 = :queCant1, " +
            " cantidad2 = :queCant2 WHERE descuentoId = :queDtoId")
    fun actualizar(queDtoId: Int, queDescuento: String, queImporte: String, queCant1: String, queCant2: String)


    @Insert
    fun insertar(dtoLinea: DtosLineasEnt)
}