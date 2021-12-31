package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "TrfFormatos", primaryKeys = ["articuloId", "tarifaId", "formatoId"])
data class TrfFormatosEnt(

    var articuloId: Int = 0,
    var tarifaId: Short = 0,
    var formatoId: Short = 0,
    var precio: String = "",
    var dto: String = "",
    var flag: Int = 0
)