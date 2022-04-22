package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.entity.CabecerasEnt
import es.albainformatica.albamobileandroid.entity.FacturasEnt


@Dao
interface FacturasDao {

    @Query("SELECT * FROM Facturas WHERE estado = 'N' OR estado = 'R'")
    fun abrirParaEnviar(): MutableList<FacturasEnt>


    @Query("SELECT * FROM Facturas WHERE estado = 'N' or estado = 'R' " +
            "OR ((firmado = 'T' OR tipoIncidencia IS NOT NULL) AND estado <> 'X')")
    fun abrirParaEnvReparto(): MutableList<FacturasEnt>


    @Query("SELECT * FROM Facturas WHERE numExport = :queNumExportacion")
    fun abrirParaEnvExp(queNumExportacion: Int): MutableList<FacturasEnt>


    @Query("UPDATE Facturas SET estado = 'X', numExport = :queNumExportacion" +
            " WHERE estado='N' OR estado='R' OR ((firmado = 'T' OR tipoincidencia IS NOT NULL) AND estado <> 'X')")
    fun marcarComoExpReparto(queNumExportacion: Int)


    @Query("UPDATE Facturas SET estado = 'X', numExport = :queNumExportacion " +
            " WHERE estado='N' OR estado='R'")
    fun marcarComoExportadas(queNumExportacion: Int)



    @Query("SELECT * FROM Facturas WHERE facturaId = :queIdDoc")
    fun cargarDoc(queIdDoc: Int): FacturasEnt


    @Query("SELECT numero FROM Facturas " +
            " WHERE almacen = :queAlmacen AND serie =  :queSerie " +
            " AND numero = :queNumero AND ejercicio = :queEjercicio")
    fun getSerieNum(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): Int



    @Update
    fun actualizar(factura: FacturasEnt)


    @Insert
    fun insertar(factura: FacturasEnt): Long
}