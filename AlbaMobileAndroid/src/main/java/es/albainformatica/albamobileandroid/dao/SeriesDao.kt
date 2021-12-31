package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosVtaSeries
import es.albainformatica.albamobileandroid.entity.SeriesEnt


@Dao
interface SeriesDao {

    @Query("SELECT ejercicio FROM series WHERE serie = :queSerie")
    fun ejercicioSerie(queSerie: String): MutableList<Short>


    @Query("SELECT Empresa FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun getEmpresa(queSerie: String, queEjercicio: Int): Short


    @Query("SELECT Flag FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun getFlag(queSerie: String, queEjercicio: Int): Int


    @Query("SELECT Factura FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun getNumFactura(queSerie: String, queEjercicio: Int): Int

    @Query("SELECT Albaran FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun getNumAlbaran(queSerie: String, queEjercicio: Int): Int

    @Query("SELECT Pedido FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun getNumPedido(queSerie: String, queEjercicio: Int): Int

    @Query("SELECT Presupuesto FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun getNumPresupuesto(queSerie: String, queEjercicio: Int): Int


    @Query("UPDATE Series SET Factura = :queNumero WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun setNumFactura(queSerie: String, queEjercicio: Int, queNumero: Int)

    @Query("UPDATE Series SET Albaran = :queNumero WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun setNumAlbaran(queSerie: String, queEjercicio: Int, queNumero: Int)

    @Query("UPDATE Series SET Pedido = :queNumero WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun setNumPedido(queSerie: String, queEjercicio: Int, queNumero: Int)

    @Query("UPDATE Series SET Presupuesto = :queNumero WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun setNumPresupuesto(queSerie: String, queEjercicio: Int, queNumero: Int)


    @Query("SELECT Serie FROM Series WHERE empresa = :queEmpresa AND ejercicio = :queEjercicio")
    fun getAllSeriesEmpresa(queEmpresa: Int, queEjercicio: Int): MutableList<DatosVtaSeries>


    @Query("SELECT Serie FROM Series WHERE Serie = :queSerie")
    fun existeSerie(queSerie: String): String

    @Query("SELECT Serie FROM Series WHERE Serie = :queSerie AND Ejercicio = :queEjercicio")
    fun existeSerieYEjerc(queSerie: String, queEjercicio: Short): String


    @Query("DELETE FROM Series WHERE ejercicio < :ejercActual OR ejercicio IS NULL")
    fun borrarAntiguas(ejercActual: Short)

    @Insert
    fun insertar(serie: SeriesEnt)


}