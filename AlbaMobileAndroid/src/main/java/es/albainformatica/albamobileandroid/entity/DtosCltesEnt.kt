package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity



@Entity(tableName = "DtosCltes", primaryKeys = ["clienteId", "idDescuento"])
data class DtosCltesEnt(

    var clienteId: Int = 0,
    var idDescuento: Int = 0,
    var dto: String = ""
)