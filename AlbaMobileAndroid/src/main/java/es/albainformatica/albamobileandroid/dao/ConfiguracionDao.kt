package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.entity.ConfiguracionEnt


@Dao
interface ConfiguracionDao {

    @Query("SELECT valor FROM Configuracion WHERE grupo = :grupo")
    fun getValor(grupo: Int): String

    @Query("SELECT descripcion FROM Configuracion WHERE grupo = :grupo")
    fun getDescripcion(grupo: Int): String


    @Query("SELECT descripcion FROM Configuracion WHERE grupo = :grupo ORDER BY valor")
    fun getListaDescr(grupo: Int): MutableList<String>

    @Query("DELETE FROM Configuracion")
    fun vaciar()

    @Query("UPDATE Configuracion SET valor = :valor WHERE grupo = :grupo")
    fun actualizar(valor: String, grupo: Int)


    @Insert
    fun insertar(configuracionEnt: ConfiguracionEnt)
}