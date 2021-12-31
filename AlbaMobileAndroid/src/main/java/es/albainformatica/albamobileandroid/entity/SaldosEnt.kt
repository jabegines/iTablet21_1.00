package es.albainformatica.albamobileandroid.entity

import androidx.room.Entity


@Entity(tableName = "Saldos", primaryKeys = ["clienteId", "empresa"])
data class SaldosEnt (
    var clienteId: Int = 0,
    var empresa: Int = 0,
    var saldo: String = "",
    var pendiente: String = "",
    var facturasPtes: Int = 0,
    var albaranesPtes: Int = 0,
    var pedidosPtes: Int = 0
)
