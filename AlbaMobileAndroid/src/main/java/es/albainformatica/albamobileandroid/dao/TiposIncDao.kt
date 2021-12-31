package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.TiposIncEnt


@Dao
interface TiposIncDao {

    @Query("SELECT tipoIncId, descripcion FROM TiposInc WHERE tipoIncId = :queIncidencia")
    fun getIncidencia(queIncidencia: Int): TiposIncEnt


    @Query("SELECT * FROM TiposInc")
    fun getAllIncidencias(): MutableList<TiposIncEnt>

    @Query("SELECT descripcion FROM TiposInc WHERE tipoIncId = :queIncidencia")
    fun dimeDescripcion(queIncidencia: Int): String


    @Query("DELETE FROM TiposInc")
    fun vaciar()

    @Insert
    fun insertar(tipoInc: TiposIncEnt)

}