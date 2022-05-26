package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.EjerciciosEnt


@Dao
interface EjerciciosDao {


    @Query("SELECT ejercicio FROM Ejercicios WHERE (julianday(fechaInicio) <= julianday(:queFecha)) AND (julianday(fechaFin) >= julianday(:queFecha))")
    fun getEjercicioActual(queFecha: String): Short


    @Query("SELECT COUNT(*) FROM Ejercicios WHERE (julianday(fechaInicio) <= julianday(:queFecha)) AND (julianday(fechaFin) >= julianday(:queFecha))")
    fun hayDatosEjercicioActual(queFecha: String): Int

    @Query("DELETE FROM Ejercicios")
    fun vaciar()

    @Insert
    fun insertar(ejercicio: EjerciciosEnt)
}