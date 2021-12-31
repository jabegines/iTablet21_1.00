package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "OftCantRangos")
data class OftCantRangosEnt (

    @PrimaryKey(autoGenerate = true)
    var oftCantRangosId: Int = 0,

    var idOferta: Int = 0,
    var articuloId: Int = 0,
    var desdeCantidad: String = "",
    var hastaCantidad: String = "",
    var precioBase: String = ""
)
