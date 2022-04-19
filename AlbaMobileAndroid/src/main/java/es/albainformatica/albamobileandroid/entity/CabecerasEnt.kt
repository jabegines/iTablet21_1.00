package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Cabeceras")
data class CabecerasEnt (

    @PrimaryKey(autoGenerate = true)
    var cabeceraId: Int = 0,

    var tipoDoc: Short = 0,
    var empresa: Short = 0,
    var ejercicio: Short = 0,
    var almacen: Short = 0,
    var serie: String = "",
    var numero: Int = 0,
    var fecha: String = "",
    var fechaEntrega: String = "",
    var hora: String = "",
    var clienteId: Int = 0,
    var aplicarIva: String = "",
    var aplicarRe: String = "",
    var bruto: String = "",
    var dto: String = "",
    var dto2: String = "",
    var dto3: String = "",
    var dto4: String = "",
    var base: String = "",
    var iva: String = "",
    var recargo: String = "",
    var total: String = "",
    var dirEnv: String = "",
    var ruta: Short = 0,
    var estado: String = "",
    var estadoInicial: String = "",
    var flag: Int = 0,
    var observ1: String = "",
    var observ2: String = "",
    var facturado: String = "",
    var numExport: Int = 0,
    var fPago: String = "",
    var tipoIncidencia: Int = 0,
    var textoIncidencia: String = "",
    var firmado: String = "",
    var fechaFirma: String = "",
    var horaFirma: String = "",
    var hojaReparto: Int = 0,
    var ordenReparto: Int = 0,
    var tipoPedido: Short = 0,
    var almDireccion: String = "",
    var ordenDireccion: String = "",
    var imprimido: String = ""
)