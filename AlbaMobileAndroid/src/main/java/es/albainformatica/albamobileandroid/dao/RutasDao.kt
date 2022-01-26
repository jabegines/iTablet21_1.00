package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.RutasEnt


@Dao
interface RutasDao {

    @Query("SELECT descripcion FROM Rutas WHERE rutaId = :queRuta")
    fun dimeNombre(queRuta: Short): String


    @Query("SELECT rutaId || ' ' || descripcion FROM Rutas ORDER BY rutaId")
    fun abrir(): Array<String>


    @Query("DELETE FROM Rutas")
    fun vaciar()

    @Insert
    fun insertar(ruta: RutasEnt)
}