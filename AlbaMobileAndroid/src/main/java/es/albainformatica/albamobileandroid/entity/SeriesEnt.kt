package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Series", primaryKeys = ["serie", "ejercicio"])
data class SeriesEnt(
    var serie: String = "",
    var ejercicio: Short = 0,
    var empresa: Short = 0,
    var factura: Int = 0,
    var albaran: Int = 0,
    var pedido: Int = 0,
    var presupuesto: Int = 0,
    var flag: Int = 0
)

