package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "CargasLineas")
data class CargasLineasEnt (

    @PrimaryKey(autoGenerate = true)
    val cargaLineaId: Int = 0,

    var cargaId: Int = 0,
    var articuloId: Int = 0,
    var lote: String = "",
    var cajas: String = "",
    var cantidad: String = ""
)