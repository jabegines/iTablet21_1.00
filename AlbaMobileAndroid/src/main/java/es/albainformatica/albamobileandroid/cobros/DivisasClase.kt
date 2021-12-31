package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import es.albainformatica.albamobileandroid.DatosDivisa
import es.albainformatica.albamobileandroid.dao.DivisasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DivisasEnt


class DivisasClase(queContexto: Context) {
    private val divisasDao: DivisasDao? = MyDatabase.getInstance(queContexto)?.divisasDao()


    fun abrir(): MutableList<DivisasEnt> {
        return divisasDao?.getAllDivisas() ?: emptyList<DivisasEnt>().toMutableList()
    }


    fun abrirParaSpinner(): MutableList<DatosDivisa> {
        return divisasDao?.getDivParaSpinner() ?: emptyList<DatosDivisa>().toMutableList()
    }


}