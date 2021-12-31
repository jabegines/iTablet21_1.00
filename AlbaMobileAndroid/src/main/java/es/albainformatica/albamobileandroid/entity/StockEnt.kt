package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "Stock", primaryKeys = ["articuloId", "empresa"])
data class StockEnt (

    var articuloId: Int = 0,
    var empresa: Short = 0,

    var ent: String = "",
    var entc: String = "",
    var entp: String = "",
    var sal: String = "",
    var salc: String = "",
    var salp: String = ""
)
