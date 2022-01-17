package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DepartamentosEnt


@Dao
interface DepartamentosDao {

    @Query("SELECT * FROM Departamentos WHERE grupoId = :queGrupo")

    fun getGrupo(queGrupo: Short): List<DepartamentosEnt>

    @Query("DELETE FROM Departamentos")
    fun vaciar()

    @Insert
    fun insertar(departamento: DepartamentosEnt)
}