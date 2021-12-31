package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Rutas")
data class RutasEnt(

    @PrimaryKey
    var rutaId: Short = 0,

    var descripcion: String = ""
)