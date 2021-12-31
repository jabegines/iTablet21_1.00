package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DepartamentosEnt


@Dao
interface DepartamentosDao {

    @Query("DELETE FROM Departamentos")
    fun vaciar()

    @Insert
    fun insertar(departamento: DepartamentosEnt)
}