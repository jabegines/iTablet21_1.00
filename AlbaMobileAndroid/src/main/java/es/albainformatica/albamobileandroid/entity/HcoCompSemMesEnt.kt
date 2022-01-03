package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "HcoCompSemMes")
data class HcoCompSemMesEnt (

    @PrimaryKey(autoGenerate = true)
    var hcoCompSemMesId: Int = 0,

    var fecha: String = "",
    var clienteId: Int = 0,
    var articuloId: Int = 0,
    var cantidad: String = "",
)
