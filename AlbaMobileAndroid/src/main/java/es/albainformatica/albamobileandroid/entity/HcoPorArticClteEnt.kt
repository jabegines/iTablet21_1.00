package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "HcoPorArticClte")
data class HcoPorArticClteEnt (

    @PrimaryKey(autoGenerate = true)
    var hcoPorcArticClteId: Int = 0,

    var articuloId: Int = 0,
    var clienteId: Int = 0,
    var tipoDoc: String = "",
    var serie: String = "",
    var numero: Int = 0,
    var ejercicio: Short = 0,
    var fecha: String = "",
    var ventas: String = "",
    var devoluciones: String = ""
)
