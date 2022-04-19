package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "LineasFras")
data class LineasFrasEnt (

    @PrimaryKey(autoGenerate = true)
    var lineaId: Int = 0,

    var facturaId: Int = 0,
    var articuloId: Int = 0,
    var codArticulo: String = "",
    var descripcion: String = "",
    var tarifaId: Short = 0,
    var precio: String = "",
    var precioII: String = "",
    var codigoIva: Short = 0,
    var cajas: String = "",
    var cajasOrg: String = "",
    var cantidad: String = "",
    var cantidadOrg: String = "",
    var piezas: String = "",
    var piezasOrg: String = "",
    var formatoId: Short = 0,
    var importe: String = "",
    var importeII: String = "",
    var dto: String = "",
    var dtoImpte: String = "",
    var dtoImpteII: String = "",
    var lote: String = "",
    var flag: Int = 0,
    var flag3: Int = 0,
    var flag5: Int = 0,
    var tasa1: String = "",
    var tasa2: String = "",
    var tipoIncId: Int = 0,
    var textoLinea: String = "",
    var modifNueva: String = "",
    var precioTarifa: String = "",
    var dtoTarifa: String = "",
    var almacenPedido: String = "",
    var ofertaId: Int = 0,
    var dtoOftVol: String = "",
    var esEnlace: String = ""
)
