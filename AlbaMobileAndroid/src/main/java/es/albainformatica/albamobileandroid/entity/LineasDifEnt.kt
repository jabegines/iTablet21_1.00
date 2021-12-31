package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LineasDiferidas")
data class LineasDifEnt (

    @PrimaryKey(autoGenerate = true)
    var lineaDiferidaId: Int = 0,

    var cabeceraId: Int = 0,
    var serie: String = "",
    var numero: Int = 0,
    var fecha: String = "",
    var linea: Short = 0,
    var articuloId: Int = 0,
    var codigo: String = "",
    var descripcion: String = "",
    var precio: String = "",
    var cajas: String = "",
    var cantidad: String = "",
    var piezas: String = "",
    var importe: String = "",
    var dto: String = "",
    var codigoIva: Short = 0,
    var flag: Int = 0,
    var flag3: Int = 0,
    var formatoId: Short = 0
)
