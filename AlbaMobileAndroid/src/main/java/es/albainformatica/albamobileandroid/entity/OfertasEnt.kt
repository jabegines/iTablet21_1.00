package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity

@Entity(tableName = "Ofertas", primaryKeys = ["articuloId", "empresa", "tarifa"])
data class OfertasEnt (

    var articuloId: Int = 0,
    var empresa: Short = 0,
    var tarifa: Short = 0,
    var precio: String = "",
    var dto: String = "",
    var formato: Short = 0,
    var tipoOferta: Short = 0,
    var idOferta: Int = 0,
    var fFinal: String = ""
)