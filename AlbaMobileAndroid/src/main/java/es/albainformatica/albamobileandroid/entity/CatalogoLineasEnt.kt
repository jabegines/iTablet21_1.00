package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "CatalogoLineas")
data class CatalogoLineasEnt (

    @PrimaryKey(autoGenerate = true)
    var catLineasId: Int = 0,

    var linea: Int = 0,
    var articuloId: Int = 0,
    var cajas: String = "0.0",
    var cantidad: String = "",
    var piezas: String = "0.0",
    var precio: String = "",
    var precioII: String = "",
    var dto: String = "",
    var importe: String = "",
    var importeII: String = "",
    var textoLinea: String = "",
    var flag: Int = 0,
    var flag5: Int = 0,
    var lote: String = "",
    var esEnlace: String = ""
)
