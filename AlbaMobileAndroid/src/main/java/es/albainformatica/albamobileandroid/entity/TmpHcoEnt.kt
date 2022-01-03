package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "TmpHco")
data class TmpHcoEnt(

    @PrimaryKey(autoGenerate = true)
    var tmpHcoId: Int = 0,

    var linea: Int = 0,
    var articuloId: Int = 0,
    var codigo: String = "",
    var descripcion: String = "",
    var cajas: String = "",
    var cantidad: String = "",
    var piezas: String = "",
    var precio: String = "",
    var precioII: String = "",
    var dto: String = "",
    var dtoImpte: String = "",
    var dtoImpteII: String = "",
    var codigoIva: Short = 0,
    var tasa1: String = "",
    var tasa2: String = "",
    var formatoId: Short = 0,
    var flag: Int = 0,
    var flag3: Int = 0,
    var flag5: Int = 0,
    var textoLinea: String = "",
    var lote: String = "",
    var almacenPedido: Short = 0,
    var incidenciaId: Int = 0
)