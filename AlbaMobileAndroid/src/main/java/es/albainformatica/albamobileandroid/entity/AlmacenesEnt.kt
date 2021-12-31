package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Almacenes")
data class AlmacenesEnt (

    @PrimaryKey
    var codigo: Int = -1,

    var descripcion: String = ""
)
