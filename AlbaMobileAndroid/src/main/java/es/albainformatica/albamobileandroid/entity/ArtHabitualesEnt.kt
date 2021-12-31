package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "ArtHabituales", primaryKeys = ["articuloId", "clienteId", "formatoId"])
data class ArtHabitualesEnt (

    var articuloId: Int = 0,
    var clienteId: Int = 0,
    var formatoId: Short = 0,
    var flag: Int = 0,
    var texto: String = ""
)