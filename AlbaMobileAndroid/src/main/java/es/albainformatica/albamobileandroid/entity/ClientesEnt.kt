package es.albainformatica.albamobileandroid.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Clientes")
data class ClientesEnt(

    @PrimaryKey
    var clienteId: Int = 0,

    var codigo: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    var nombre: String = "",
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    var nombreComercial: String = "",
    var cif: String = "",
    var direccion: String = "",
    var localidad: String = "",
    var cPostal: String = "",
    var provincia: String = "",
    var aplIva: String = "",
    var aplRec: String = "",
    var tipoIva: Short = 0,
    var tarifaId: Short = 0,
    var tarifaDtoId: Short = 0,
    var tarifaPiezas: Short = 0,
    var fPago: String = "",
    var rutaId: Short = 0,
    var riesgo: String = "",
    var pendiente: String = "",
    var flag: Int = 0,
    var flag2: Int = 0,
    var estado: String = "",
    var ramo: Short = 0,
    var numExport: Int = 0,
    var tieneIncid: String = "",
    var maxDias: Int = 0,
    var maxFrasPdtes: Int = 0
)
