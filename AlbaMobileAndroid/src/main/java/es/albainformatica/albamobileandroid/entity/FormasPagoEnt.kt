package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "FormasPago")
data class FormasPagoEnt(

    @PrimaryKey
    var codigo: String = "",

    var clave: String = "",
    var orden: Short = 0,
    var descripcion: String = "",
    var generaCobro: String = "",
    var generaVtos: String = "",
    var numVtos: Short = 0,
    var primerVto: Short = 0,
    var entrePagos: Short = 0,
    var pideDivisas: String = "",
    var pideAnotacion: String = "",
    var anotacion: String = ""
)