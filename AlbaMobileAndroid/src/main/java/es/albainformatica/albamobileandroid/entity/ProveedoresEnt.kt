package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Proveedores")
data class ProveedoresEnt (

    @PrimaryKey
    var proveedorId: Int = 0,

    var nombre: String = "",
    var cif: String = ""
)
