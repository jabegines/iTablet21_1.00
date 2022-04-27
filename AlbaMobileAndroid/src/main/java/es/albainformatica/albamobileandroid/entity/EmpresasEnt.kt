package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Empresas")
data class EmpresasEnt (

    @PrimaryKey
    var codigo: Int = -1,

    var nombreFiscal: String = "",
    var nombrecomercial: String = "",
    var venderIvaIncl: String = ""
)
