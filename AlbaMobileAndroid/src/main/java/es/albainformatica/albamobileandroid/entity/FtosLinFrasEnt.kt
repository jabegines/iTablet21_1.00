package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "FtosLinFras")
data class FtosLinFrasEnt (

    @PrimaryKey(autoGenerate = true)
    var ftoLineaId: Int = 0,

    var lineaId: Int = 0,
    var articuloId: Int = 0,
    var formatoId: Short = 0,
    var cajas: String = "",
    var cantidad: String = "",
    var piezas: String = "",
    var precio: String = "",
    var dto: String = "",
    var textoLinea: String = "",
    var flag: Int = 0,
    var flag5: Int = 0,
    var borrar: String = ""
)
