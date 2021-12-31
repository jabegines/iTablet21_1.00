package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.RatingGruposEnt


@Dao
interface RatingGruposDao {

    @Query("SELECT dto FROM RatingGrupos WHERE grupo = :queGrupo AND departamento = :queDepartamento " +
            " AND almacen = :queAlmacen AND clienteId = :queCliente AND julianday(inicio) <= julianday(:queFecha) " +
            " AND julianday(fin) >= julianday(:queFecha)")
    fun getDescuento(queGrupo: Short, queDepartamento: Short, queAlmacen: Short, queCliente: Int, queFecha: String): String


    @Query("SELECT dto FROM RatingGrupos WHERE grupo = :queGrupo AND departamento = :queDepartamento " +
            " AND almacen = :queAlmacen AND ramo = :queRamo AND tarifaId = :queTarifa " +
            " AND julianday(inicio) <= julianday(:queFecha) AND julianday(fin) >= julianday(:queFecha)")
    fun getDtoRamoTarifa(queGrupo: Short, queDepartamento: Short, queAlmacen: Short, queRamo: Short, queTarifa: Short, queFecha: String): String


    @Query("DELETE FROM RatingGrupos")
    fun vaciar()

    @Insert
    fun insertar(rating: RatingGruposEnt)
}