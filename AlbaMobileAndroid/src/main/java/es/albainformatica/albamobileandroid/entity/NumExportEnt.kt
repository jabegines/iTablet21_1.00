package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "NumExport")
data class NumExportEnt (

    @PrimaryKey
    var numExport: Int = 0,

    var fecha: String = "",
    var hora: String = ""
)
