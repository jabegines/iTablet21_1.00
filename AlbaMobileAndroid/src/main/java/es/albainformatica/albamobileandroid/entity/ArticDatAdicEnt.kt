package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "ArticDatAdic", primaryKeys = ["articuloId", "valor"])
data class ArticDatAdicEnt (

    var articuloId: Int = 0,
    var valor: Int = 0,
    var cadena: String = ""
)
