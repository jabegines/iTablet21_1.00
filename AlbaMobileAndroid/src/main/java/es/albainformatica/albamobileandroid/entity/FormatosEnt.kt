package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Formatos")
data class FormatosEnt(

    @PrimaryKey
    var formatoId: Short = 0,

    var descripcion: String = "",
    var flag: Int = 0,
    var dosis1: String = ""
)