package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "OftVolRangos")
data class OftVolRangosEnt (

    @PrimaryKey(autoGenerate = true)
    var oftVolRangosId: Int = 0,

    var idOferta: Int = 0,
    var desdeImpte: Int = 0,
    var hastaImpte: Int = 0,
    var descuento: String = ""
)
