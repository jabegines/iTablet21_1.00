package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.DatosVtaFtos
import es.albainformatica.albamobileandroid.dao.FormatosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.FormatosEnt


class Formatos(val contexto: Context) {
    val formatosDao: FormatosDao? = MyDatabase.getInstance(contexto)?.formatosDao()

    lateinit var lFormatos: List<FormatosEnt>
    lateinit var lFtosCat: List<DatosVtaFtos>


    fun abrirFormatos(queArticulo: Int, queCliente: Int): Boolean {
        lFtosCat = formatosDao?.abrirFtosParaCat(queArticulo, queCliente) ?: emptyList<DatosVtaFtos>().toMutableList()
        return (lFtosCat.isNotEmpty())
    }


    fun todosLosFormatos(): Boolean {
        lFormatos = formatosDao?.getAllFormatos() ?: emptyList<FormatosEnt>().toMutableList()
        return (lFormatos.isNotEmpty())
    }


}