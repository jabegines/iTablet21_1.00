package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "RegistroDeEventos")
data class RegistroDeEventosEnt (

    @PrimaryKey(autoGenerate = true)
    var eventoId: Int = 0,

    var fecha: String = "",
    var hora: String = "",
    var ordenDiarioPuesto: Int = 0,
    var usuario: Short = 0,
    var almacen: Short = 0,
    var puesto: Short = 0,
    var codigoEvento: String = "",
    var ip: String = "",
    var ejercicio: Short = 0,
    var empresa: Short = 0,
    var descrEvento: String = "",
    var textoEvento: String = "",
    var referenciaAnterior: String = "",
    var huellaRefAnterior: String = "",
    var huella: String = "",
    var firma: String = "",
    var firmaCadena: String = "",
    var firmaVersion: String = "",
    var numExport: Int = 0,
    var estado: String = ""
)