package es.albainformatica.albamobileandroid.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Articulos")
data class ArticulosEnt (

    @PrimaryKey
    var articuloId: Int = 0,

    var codigo: String = "",
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    var descripcion: String = "",
    var tipoIva: Short = 0,
    var grupoId: Short = 0,
    var departamentoId: Short = 0,
    var proveedorId: Int = 0,
    var costo: String = "",
    var uCaja: String = "",
    var medida: String = "",
    var flag1: Int = 0,
    var flag2: Int = 0,
    var peso: String = "",
    var tasa1: String = "",
    var tasa2: String = "",
    var enlace: Int = 0
)
