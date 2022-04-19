package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "NotasCltes")
data class NotasCltesEnt(

    @PrimaryKey(autoGenerate = true)
    var notaId: Int = 0,

    var clienteId: Int = 0,
    var nota: String = "",
    var fecha: String = "",
    var estado: String = "",
    var numExport: Int = 0
)