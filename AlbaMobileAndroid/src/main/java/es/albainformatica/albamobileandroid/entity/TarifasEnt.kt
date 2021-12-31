package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "Tarifas", primaryKeys = ["articuloId", "tarifaId"])
data class TarifasEnt(

    var articuloId: Int = 0,
    var tarifaId: Short = 0,
    var precio: String = "",
    var dto: String = "",
    var flag: Short = 0
)