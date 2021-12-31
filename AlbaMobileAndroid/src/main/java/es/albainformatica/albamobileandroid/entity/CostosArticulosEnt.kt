package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "CostosArticulos", primaryKeys = ["articuloId", "empresa"])
data class CostosArticulosEnt(
    var articuloId: Int = 0,
    var empresa: Int = 0,
    var costo: String = ""
)