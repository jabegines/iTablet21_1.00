package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Pendiente")
data class PendienteEnt (

    @PrimaryKey(autoGenerate = true)
    var pendienteId: Int = 0,

    var clienteId: Int = 0,
    var ejercicio: Short = 0,
    var empresa: Short = 0,
    var almacen: Short = 0,
    var tipoDoc: Short = 0,
    var fPago: String = "",
    var fechaDoc: String = "",
    var serie: String = "",
    var numero: Int = 0,
    var importe: String = "",
    var cobrado: String = "",
    var fechaVto: String = "",
    var estado: String = "",
    var enviar: String = "",
    var cAlmacen: String = "",
    var cPuesto: String = "",
    var cApunte: String = "",
    var cEjercicio: String = "",
    var numExport: Int = 0,
    var flag: Int = 0,
    var anotacion: String = "",
    var hoja: Int = 0,
    var orden: Int = 0
)