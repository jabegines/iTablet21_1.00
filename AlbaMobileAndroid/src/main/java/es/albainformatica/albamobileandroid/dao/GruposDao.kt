package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.GruposParaCat
import es.albainformatica.albamobileandroid.entity.GruposEnt


@Dao
interface GruposDao {

    @Query("SELECT A.codigo, A.descripcion, " +
            " (SELECT COUNT(*) FROM Departamentos WHERE grupoId = A.codigo) numDepartamentos" +
            " FROM Grupos A " +
            " WHERE numDepartamentos > 0")
    fun abrirParaCatalogo(): List<GruposParaCat>


    @Query("SELECT * FROM Grupos")
    fun getAllGrupos(): List<GruposEnt>


    @Query("DELETE FROM Grupos")
    fun vaciar()


    @Insert
    fun insertar(grupo: GruposEnt)
}