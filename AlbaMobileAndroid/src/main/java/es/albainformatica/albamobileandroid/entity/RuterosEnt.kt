package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Rutero")
data class RuterosEnt (

    @PrimaryKey(autoGenerate = true)
    var ruteroId: Int = 0,

    var rutaId: Short = 0,
    var orden: Short = 0,
    var clienteId: Int = 0
)
