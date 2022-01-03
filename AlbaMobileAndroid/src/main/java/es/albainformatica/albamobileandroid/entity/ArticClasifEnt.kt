package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "ArticClasif", primaryKeys = ["articuloId", "clasificadorId"])
data class ArticClasifEnt(

    var articuloId: Int = 0,
    var clasificadorId: Int = 0,
    var orden: Int = 0
)