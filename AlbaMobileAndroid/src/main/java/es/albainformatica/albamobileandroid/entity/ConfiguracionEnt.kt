package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Configuracion")
data class ConfiguracionEnt (

    @PrimaryKey(autoGenerate = true)
    var configuracionId: Int = 0,

    var grupo: Int = 0,
    var descripcion: String = "",
    var valor: String = ""
)