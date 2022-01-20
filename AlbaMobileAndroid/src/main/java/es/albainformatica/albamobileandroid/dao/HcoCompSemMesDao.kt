package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosHcoCompSemMes
import es.albainformatica.albamobileandroid.entity.HcoCompSemMesEnt


@Dao
interface HcoCompSemMesDao {

    @Query("SELECT SUM(suma1) AS suma1, SUM(suma2) AS suma2, codigo, descripcion FROM (" +
            " SELECT SUM(A.cantidad) AS suma1, 0 AS suma2, B.codigo, B.descripcion FROM HcoCompSemMes A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente " +
            " AND julianday(A.fecha) >= julianday(:queFechaMenos6) " +
            " AND julianday(A.fecha) <= julianday(:queFechaHoy) " +
            " GROUP BY B.codigo, B.descripcion " +
            " UNION ALL " +
            " SELECT 0 AS suma1, SUM(A.cantidad) AS suma2, B.codigo, B.descripcion FROM HcocompSemMes A" +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente " +
            " AND julianday(A.fecha) >= julianday(:queFechaMenos13) " +
            " AND julianday(A.fecha) <= julianday(:queFechaMenos7) " +
            " GROUP BY B.codigo, B.descripcion" +
            " ) AS consulta GROUP BY codigo")
    fun abrir(queCliente: Int, queFechaMenos6: String, queFechaHoy: String,
              queFechaMenos13: String, queFechaMenos7: String): List<DatosHcoCompSemMes>


    @Query("SELECT hcoCompSemMesId FROM HcoCompSemMes")
    fun existe(): Int

    @Query("DELETE FROM HcoCompSemMes")
    fun vaciar()

    @Insert
    fun insertar(hcoCompSemMes: HcoCompSemMesEnt)
}