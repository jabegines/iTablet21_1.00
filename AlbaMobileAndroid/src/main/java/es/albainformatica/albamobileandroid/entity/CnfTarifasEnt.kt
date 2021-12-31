package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "CnfTarifas")
data class CnfTarifasEnt(

    @PrimaryKey
    var codigo: Short = 0,

    var descrTarifa: String = "",
    var precios: String = "",
    var flag: Int = 0
)