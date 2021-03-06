package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.dao.LotesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.LotesEnt


class LotesClase(queContexto: Context) {
    private val lotesDao: LotesDao? = MyDatabase.getInstance(queContexto)?.lotesDao()

    lateinit var lLotes: List<LotesEnt>


    fun actStockLote(queArticulo: Int, queCantidad: Double, queLote: String, queEmpresa: Short) {

        // Vemos si el artículo está en la tabla Lotes. Si no está lo insertamos, aunque el lote esté en blanco, ya que nos vendrá
        // bien a la hora de realizar el fin de día en las cargas.
        val loteEnt = lotesDao?.existeArtYLote(queArticulo, queLote, queEmpresa) ?: LotesEnt()
        if (loteEnt.stock == "") loteEnt.stock = "0.0"
        val dStock = loteEnt.stock.toDouble() - queCantidad

        if (loteEnt.loteId > 0) lotesDao?.actualizarStock(queArticulo, dStock.toString(), queLote, queEmpresa)
        else {
            loteEnt.articuloId = queArticulo
            loteEnt.empresa = queEmpresa
            loteEnt.lote = queLote
            loteEnt.stock = dStock.toString()
            lotesDao?.insertar(loteEnt)
        }
    }


    fun dimeStockLote(queArticulo: Int, queLote: String): Double {
        val sStock = lotesDao?.getStockLote(queArticulo, queLote) ?: "0.0"
        return sStock.toDouble()
    }


    fun getAllLotesArticulo(queArticulo: Int, sumarEmpresas: Boolean): List<LotesEnt> {
        lLotes = if (sumarEmpresas) lotesDao?.getAllLotesArtSum(queArticulo) ?: emptyList<LotesEnt>().toMutableList()
        else lotesDao?.getAllLotesArticulo(queArticulo) ?: emptyList<LotesEnt>().toMutableList()

        return lLotes
    }


    fun abrirLotesFinDia(): List<LotesEnt> {
        lLotes = lotesDao?.getAllLotes() ?: emptyList<LotesEnt>().toMutableList()
        return lLotes
    }


}