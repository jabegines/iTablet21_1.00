package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Ivas")
data class IvasEnt (

    @PrimaryKey
    var codigo: Short = 0,

    var tipo: Short = 0,
    var porcIva: String = "",
    var porcRe: String = ""
)
