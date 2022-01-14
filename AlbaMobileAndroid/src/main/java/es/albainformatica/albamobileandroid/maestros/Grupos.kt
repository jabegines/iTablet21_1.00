package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import es.albainformatica.albamobileandroid.GruposParaCat
import es.albainformatica.albamobileandroid.dao.GruposDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.GruposEnt

/**
 * Created by jabegines on 05/05/2014.
 */
class Grupos(contexto: Context) {
    private val gruposDao: GruposDao? = MyDatabase.getInstance(contexto)?.gruposDao()

    lateinit var lGrupos: List<GruposEnt>
    lateinit var lGrupCat: List<GruposParaCat>


    fun abrir(): Boolean {

        lGrupos = gruposDao?.getAllGrupos() ?: emptyList<GruposEnt>().toMutableList()
        return (lGrupos.count() > 0)
    }

    fun abrirParaCatalogo(): Boolean {

        lGrupCat = gruposDao?.abrirParaCatalogo() ?: emptyList<GruposParaCat>().toMutableList()
        return (lGrupCat.count() > 0)
    }

    //val imagen: String
    //    get() = "GRP_$codigo.jpg"

}