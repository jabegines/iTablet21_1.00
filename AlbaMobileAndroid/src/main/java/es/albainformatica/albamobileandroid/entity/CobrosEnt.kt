package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Cobros")
data class CobrosEnt (

    @PrimaryKey(autoGenerate = true)
    var cobroId: Int = 0,

    var clienteId: Int = 0,
    var tipoDoc: Short = 0,
    var almacen: Short = 0,
    var serie: String = "",
    var numero: Int = 0,
    var ejercicio: Short = 0,
    var empresa: Short = 0,
    var fechaCobro: String = "",
    var cobro: String = "",
    var fPago: String = "",
    var divisa: String = "",
    var anotacion: String = "",
    var codigo: String = "",
    var estado: String = "",
    var vAlmacen: String = "",
    var vPuesto: String = "",
    var vApunte: String = "",
    var vEjercicio: String = "",
    var numExport: Int = 0,
    var matricula: String = ""
)
