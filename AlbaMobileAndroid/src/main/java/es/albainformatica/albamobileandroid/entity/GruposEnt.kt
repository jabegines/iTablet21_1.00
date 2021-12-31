package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Grupos")
data class GruposEnt (

    @PrimaryKey
    var codigo: Int = 0,

    var descripcion: String = ""
)
