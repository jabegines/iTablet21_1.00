package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Lotes")
data class LotesEnt(

    @PrimaryKey(autoGenerate = true)
    var loteId: Int = 0,

    var empresa: Short = 0,
    var articuloId: Int = 0,
    var lote: String = "",
    var stock: String = "",
    var stockPiezas: String = "",
    var flag: Int = 0
)