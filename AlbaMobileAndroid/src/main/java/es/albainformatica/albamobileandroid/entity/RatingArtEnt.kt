package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "RatingArt")
data class RatingArtEnt (

    @PrimaryKey(autoGenerate = true)
    var ratingArtId: Int = 0,

    var articuloId: Int = 0,
    var almacen: Short = 0,
    var clienteId: Int = 0,
    var ramoId: Short = 0,
    var tarifaId: Short = 0,
    var inicio: String = "",
    var fin: String = "",
    var formatoId: Short = 0,
    var precio: String = "",
    var dto: String = "",
    var flag: Int = 0,
)
