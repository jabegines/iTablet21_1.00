package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.RatingProvEnt


@Dao
interface RatingProvDao {

    @Query("SELECT dto FROM RatingProv WHERE proveedorId = :queProveedor AND almacen = :queAlmacen" +
            " AND clienteId = :queCliente AND julianday(inicio) <= julianday(:queFecha)" +
            " AND julianday(fin) >= julianday(:queFecha)")
    fun getDtoClteProv(queProveedor: Int, queAlmacen: Short, queCliente: Int, queFecha: String): String


    @Query("SELECT dto FROM RatingProv WHERE proveedorId = :queProveedor AND almacen = :queAlmacen " +
            " AND ramoId = :queRamo AND tarifa = :queTarifa AND julianday(inicio) <= julianday(:queFecha)" +
            " AND julianday(fin) >= julianday(:queFecha)")
    fun getDtoRamoTrfProv(queProveedor: Int, queAlmacen: Short, queRamo: Short, queTarifa: Short, queFecha: String): String


    @Query("DELETE FROM RatingProv")
    fun vaciar()

    @Insert
    fun insertar(rating: RatingProvEnt)
}