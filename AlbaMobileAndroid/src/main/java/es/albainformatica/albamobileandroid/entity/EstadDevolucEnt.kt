package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "EstadDevoluc")
data class EstadDevolucEnt (

    @PrimaryKey(autoGenerate = true)
    var estadDevolucId: Int = 0,

    var clienteId: Int = 0,
    var articuloId: Int = 0,
    var porcDevol: String = ""
)
