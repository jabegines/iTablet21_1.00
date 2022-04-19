package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "DtosLinFras")
data class DtosLinFrasEnt(

    @PrimaryKey(autoGenerate = true)
    var descuentoId: Int = 0,

    var lineaId: Int = 0,
    var orden: Short = 0,
    var descuento: String = "",
    var importe: String = "",
    var cantidad1: String = "",
    var cantidad2: String = "",
    var desdeRating: String = ""
)
