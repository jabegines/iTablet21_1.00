package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "ContactosCltes")
data class ContactosCltesEnt (

    @PrimaryKey(autoGenerate = true)
    var contactoClteId: Int = 0,

    var clienteId: Int = 0,
    var almacen: Short = 0,
    var orden: Short = 0,
    var sucursal: Short = 0,
    var nombre: String = "",
    var telefono1: String = "",
    var telefono2: String = "",
    var obs1: String = "",
    var eMail: String = "",
    var flag: Int = 0,
    var estado: String = "",
    var numExport: Int = 0
)
