package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Historico")
data class HistoricoEnt(

    @PrimaryKey(autoGenerate = true)
    var historicoId: Int = 0,

    var clienteId: Int = 0,
    var articuloId: Int = 0,
    var cajas: String = "",
    var cantidad: String = "",
    var piezas: String = "",
    var precio: String = "",
    var dto: String = "",
    var formatoId: Short = 0,
    var fecha: String = ""
)
