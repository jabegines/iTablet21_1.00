package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "OftVolumen")
data class OftVolumenEnt (

    @PrimaryKey
    var oftVolumenId: Int = 0,
    var almacen: Int = 0,
    var articuloDesct: Int = 0,
    var tarifa: Short = 0
)
