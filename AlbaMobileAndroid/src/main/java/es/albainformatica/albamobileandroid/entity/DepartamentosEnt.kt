package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "Departamentos", primaryKeys = ["grupoId", "departamentoId"])
data class DepartamentosEnt(

    var grupoId: Short = 0,
    var departamentoId: Short = 0,
    var descripcion: String = ""
)