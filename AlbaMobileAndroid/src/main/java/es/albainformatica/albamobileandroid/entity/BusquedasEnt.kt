package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Busquedas")
data class BusquedasEnt (

    @PrimaryKey
    var clave: String = "",

    var articuloId: Int = 0,
    var tipo: Short = 0,
    var tcaja: String = "",
    var ucaja: String = ""
)
