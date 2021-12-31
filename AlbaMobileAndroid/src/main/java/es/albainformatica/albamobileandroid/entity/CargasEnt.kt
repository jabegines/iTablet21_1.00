package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Cargas")
data class CargasEnt (

    @PrimaryKey(autoGenerate = true)
    var cargaId: Int = 0,

    var empresa: Short = 0,
    var fecha: String = "",
    var hora: String = "",
    var esFinDeDia: String = "",
    var estado: String = "",
    var numExport: Int = 0,
    var matricula: String = ""
)