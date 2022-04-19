package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.CabecerasEnt
import es.albainformatica.albamobileandroid.entity.FacturasEnt


@Dao
interface FacturasDao {

    @Query("SELECT * FROM Facturas WHERE facturaId = :queIdDoc")
    fun cargarDoc(queIdDoc: Int): FacturasEnt


    @Query("SELECT numero FROM Facturas " +
            " WHERE almacen = :queAlmacen AND serie =  :queSerie " +
            " AND numero = :queNumero AND ejercicio = :queEjercicio")
    fun getSerieNum(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): Int



    @Insert
    fun insertar(factura: FacturasEnt)
}