package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "HistMes")
data class HistMesEnt (

    @PrimaryKey(autoGenerate = true)
    var histMesId: Int = 0,

    var clienteId: Int = 0,
    var articuloId: Int = 0,
    var mes: Int = 0,
    var cantidad: String = "",
    var cantidadAnt: String = "",
    var importe: String = "",
    var importeAnt: String = "",
    var cajas: String = "",
    var cajasAnt: String = "",
    var piezas: String = "",
    var piezasAnt: String = "",
)
