package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "RatingGrupos")
data class RatingGruposEnt (

    @PrimaryKey(autoGenerate = true)
    var ratingGruposId: Int = 0,

    var grupo: Short = 0,
    var departamento: Short = 0,
    var almacen: Short = 0,
    var clienteId: Int = 0,
    var ramo: Short = 0,
    var tarifaId: Short = 0,
    var inicio: String = "",
    var fin: String = "",
    var dto: String = ""
)
