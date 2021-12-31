package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Ejercicios")
data class EjerciciosEnt (

    @PrimaryKey
    var ejercicio: Short = -1,
    var descripcion: String = "",
    var fechaInicio: String = "",
    var fechaFin: String = ""
)
