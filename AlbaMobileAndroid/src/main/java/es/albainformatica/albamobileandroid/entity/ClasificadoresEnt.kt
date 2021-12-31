package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Clasificadores")
data class ClasificadoresEnt(

    @PrimaryKey
    var clasificadorId: Int = 0,

    var descripcion: String = "",
    var padre: Int = 0,
    var nivel: Int = 0,
    var orden: Int = 0,
    var flag: Int = 0
)