package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Divisas")
data class DivisasEnt (

    @PrimaryKey
    var codigo: String = "",

    var clave: String = "",
    var orden: Short = 0,
    var descripcion: String = "",
    var pideAnotacion: String = "",
    var anotacion: String = ""
)
