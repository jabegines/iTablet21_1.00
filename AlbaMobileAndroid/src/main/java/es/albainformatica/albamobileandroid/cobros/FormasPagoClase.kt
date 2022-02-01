package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import es.albainformatica.albamobileandroid.DatosFPago
import es.albainformatica.albamobileandroid.dao.FormasPagoDao
import es.albainformatica.albamobileandroid.database.MyDatabase


class FormasPagoClase(queContexto: Context) {
    private val fPagoDao: FormasPagoDao? = MyDatabase.getInstance(queContexto)?.formasPagoDao()



    fun abrir(): List<DatosFPago> {
        return fPagoDao?.getAllFPago() ?: emptyList<DatosFPago>().toMutableList()
    }


    fun abrirSoloContado(): List<DatosFPago> {
        return fPagoDao?.abrirSoloContado() ?: emptyList<DatosFPago>().toMutableList()
    }


    fun fPagoEsContado(queFPago: String): Boolean {
        val generaCobro = fPagoDao?.getGeneraCobro(queFPago) ?: "F"
        return (generaCobro == "T")
    }


    fun getDescrFPago(queFPago: String): String {
        return fPagoDao?.getDescrFPago(queFPago) ?: ""
    }

}