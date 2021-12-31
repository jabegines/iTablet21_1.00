package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "DocsCabPies", primaryKeys = ["empresa", "valor"])
class DocsCabPiesEnt (
    var empresa: Short = 0,
    var valor: String = "",
    var cadena: String = "",
    var entero: Int = 0
)
