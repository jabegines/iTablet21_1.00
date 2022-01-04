package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.dao.RutasDao
import es.albainformatica.albamobileandroid.database.MyDatabase

/**
 * Created by jabegines on 11/10/13.
 */
class Rutas(contexto: Context)  {
    private val rutasDao: RutasDao? = MyDatabase.getInstance(contexto)?.rutasDao()


    fun abrir(): Array<String> {
        return rutasDao?.abrir() ?: emptyList<String>().toTypedArray()
    }


    fun dimeNombre(queRuta: String): String {
        return rutasDao?.dimeNombre(queRuta) ?: ""
    }
}