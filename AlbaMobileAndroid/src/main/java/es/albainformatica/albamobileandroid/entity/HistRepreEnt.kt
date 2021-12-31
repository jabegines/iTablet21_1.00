package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "HistRepre")
data class HistRepreEnt (

    @PrimaryKey(autoGenerate = true)
    var histRepreId: Int = 0,

    var representanteId: Int = 0,
    var importe: String = "",
    var mes: Int = 0,
    var anyo: Int = 0
)
