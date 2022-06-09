package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.RegistroDeEventosEnt

@Dao
interface RegistroDeEventosDao {

    @Query("SELECT MAX(ordenDiarioPuesto) FROM RegistroDeEventos " +
            " WHERE fecha = :queFecha")
    fun getUltimoOrdenDiario(queFecha: String): Int

    @Insert
    fun insertar(evento: RegistroDeEventosEnt)
}