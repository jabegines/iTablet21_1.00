package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.ProveedoresEnt


@Dao
interface ProveedoresDao {

    @Query("SELECT nombre FROM Proveedores WHERE proveedorId = :queProveedor")
    fun getNombreProv(queProveedor: Int): String

    @Query("DELETE FROM Proveedores")
    fun vaciar()

    @Insert
    fun insertar(proveedor: ProveedoresEnt)
}