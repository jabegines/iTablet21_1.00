package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "TiposInc")
data class TiposIncEnt(

    @PrimaryKey
    var tipoIncId: Int = 0,

    var descripcion: String = ""
)