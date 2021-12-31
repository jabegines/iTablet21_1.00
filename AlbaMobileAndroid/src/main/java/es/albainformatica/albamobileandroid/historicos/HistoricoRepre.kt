package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import es.albainformatica.albamobileandroid.dao.HistRepreDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.HistRepreEnt

class HistoricoRepre(contexto: Context) {
    private val hcoRepreDao: HistRepreDao? = MyDatabase.getInstance(contexto)?.histRepreDao()


    fun abrir(): MutableList<HistRepreEnt> {
        return hcoRepreDao?.getAllHco() ?: emptyList<HistRepreEnt>().toMutableList()
    }

}