package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "CabDiferidas")
data class CabDiferidasEnt (

    @PrimaryKey(autoGenerate = true)
    var cabDiferidaId: Int = 0,

    var serie: String = "",
    var numero: Int = 0,
    var ejercicio: Short = 0,
    var empresa: Short = 0,
    var fecha: String = "",
    var clienteId: Int = 0,
    var aplIva: String = "",
    var aplRec: String = "",
    var bruto: String = "",
    var dto: String = "",
    var base: String = "",
    var iva: String= "",
    var recargo: String = "",
    var total: String = "",
    var flag: Int = 0,
    var obs1: String = "",
    var obs2: String = ""
)
