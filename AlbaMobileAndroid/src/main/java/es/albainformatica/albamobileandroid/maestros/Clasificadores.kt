package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.ClasifParaCat
import es.albainformatica.albamobileandroid.dao.ClasificadoresDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ClasificadoresEnt


/**
 * Created by jabegines on 13/06/2014.
 */
class Clasificadores(contexto: Context) {
    private val clasificadoresDao: ClasificadoresDao? = MyDatabase.getInstance(contexto)?.clasificadoresDao()

    lateinit var lClasificadores: List<ClasificadoresEnt>
    lateinit var lClasCat: List<ClasifParaCat>


    // En la tabla de clasificadores el flag 0 nos indicará que el registro pertenece a la clasificación avanzada
    // y el flag 1 nos indicará que el registro es un catálogo.
    fun abrir(quePadre: Int, queNivel: Int): Boolean {
        lClasificadores = clasificadoresDao?.getClasifPadre(quePadre, queNivel) ?: emptyList<ClasificadoresEnt>().toMutableList()
        return (lClasificadores.isNotEmpty())
    }


    fun abrirCatalogos(): Boolean {
        lClasificadores = clasificadoresDao?.abrirCatalogos() ?: emptyList<ClasificadoresEnt>().toMutableList()
        return (lClasificadores.isNotEmpty())
    }


    fun abrirBioCatalogo(): Boolean {
        lClasCat = clasificadoresDao?.abrirBioCatalogo() ?: emptyList<ClasifParaCat>().toMutableList()
        return (lClasCat.isNotEmpty())
    }


    //val imagen: String
    //    get() = "CLS_" + getCodigo() + ".jpg"

}