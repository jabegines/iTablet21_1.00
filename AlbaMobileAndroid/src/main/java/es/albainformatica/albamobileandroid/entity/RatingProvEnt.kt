package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "RatingProv")
data class RatingProvEnt (

    @PrimaryKey(autoGenerate = true)
    var ratingProvId: Int = 0,

    var proveedorId: Int = 0,
    var almacen: Int = 0,
    var clienteId: Int = 0,
    var ramoId: Short = 0,
    var tarifa: Short = 0,
    var inicio: String = "",
    var fin: String = "",
    var dto: String = ""
)
