package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "DireccionesCltes")
data class DireccCltesEnt(

    @PrimaryKey(autoGenerate = true)
    var direccionId: Int = 0,

    var clienteId: Int = 0,
    var almacen: Short = 0,
    var orden: Short = 0,
    var sucursal: Short = 0,
    var direccion: String = "",
    var localidad: String = "",
    var provincia: String = "",
    var cPostal: String = "",
    var pais: String = "",
    var direccionDoc: String = "",
    var direccionMerc: String = "",
    var estado: String = "",
    var numExport: Int = 0
)