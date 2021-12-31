package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.dao.FormasPagoDao
import es.albainformatica.albamobileandroid.database.MyDatabase


class FormasPagoClase(queContexto: Context) {
    private val fPagoDao: FormasPagoDao? = MyDatabase.getInstance(queContexto)?.formasPagoDao()

    var cursor: Cursor? = null


    fun abrir() {
        cursor = fPagoDao?.getAllFPago()
        cursor?.moveToFirst()
    }


    fun abrirSoloContado() {
        cursor = fPagoDao?.abrirSoloContado()
        cursor?.moveToFirst()
    }


    fun fPagoEsContado(queFPago: String): Boolean {
        val generaCobro = fPagoDao?.getGeneraCobro(queFPago) ?: "F"
        return (generaCobro == "T")
    }


    fun getDescrFPago(queFPago: String): String {
        return fPagoDao?.getDescrFPago(queFPago) ?: ""
    }

}