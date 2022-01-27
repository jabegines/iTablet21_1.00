package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DepartParaCat
import es.albainformatica.albamobileandroid.entity.DepartamentosEnt


@Dao
interface DepartamentosDao {

    @Query("SELECT A.departamentoId, A.descripcion, " +
            " (SELECT COUNT(*) FROM Articulos WHERE grupoId = :queGrupo AND departamentoId = A.departamentoId) numArticulos " +
            " FROM Departamentos A " +
            " WHERE A.grupoId = :queGrupo " +
            " ORDER BY A.departamentoId")
    fun abrirParaCatalogo(queGrupo: Short): List<DepartParaCat>


    @Query("SELECT * FROM Departamentos WHERE grupoId = :queGrupo")
    fun getGrupo(queGrupo: Short): List<DepartamentosEnt>

    @Query("DELETE FROM Departamentos")
    fun vaciar()

    @Insert
    fun insertar(departamento: DepartamentosEnt)
}