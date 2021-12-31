package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.dao.AlmacenDao
import es.albainformatica.albamobileandroid.database.MyDatabase


class AlmacenesClase(queContexto: Context) {
    private val almacenesDao: AlmacenDao? = MyDatabase.getInstance(queContexto)?.almacenesDao()


    fun existe(codAlmacen: String): Boolean {
        val queDescr = almacenesDao?.getCodAlmacen(codAlmacen.toInt()) ?: ""
        return (queDescr != "")
    }
}