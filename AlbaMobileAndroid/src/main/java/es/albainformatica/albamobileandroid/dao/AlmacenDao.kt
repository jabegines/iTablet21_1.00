package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.AlmacenesEnt

@Dao
interface AlmacenDao {

    @Query("SELECT descripcion FROM Almacenes WHERE codigo = :queCodigo")
    fun getCodAlmacen(queCodigo: Int): String

    //@Query("SELECT descripcion FROM Almacenes WHERE codigo = :queCodigo")
    //fun getCodigo(queCodigo: Int): String

    //@Query("SELECT descripcion FROM Almacenes WHERE codigo = :queCodigo")
    //fun existeCodigo(queCodigo: Int): String

    @Query("DELETE FROM Almacenes")
    fun vaciar()

    @Insert
    fun insertar(almacen: AlmacenesEnt)
}