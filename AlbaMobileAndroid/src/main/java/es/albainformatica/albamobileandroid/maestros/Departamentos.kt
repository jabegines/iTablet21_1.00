package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.DepartParaCat
import es.albainformatica.albamobileandroid.dao.DepartamentosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DepartamentosEnt

/**
 * Created by jabegines on 06/05/2014.
 */
class Departamentos(contexto: Context) {
    private val departamentosDao: DepartamentosDao? = MyDatabase.getInstance(contexto)?.departamentosDao()

    lateinit var lDepartamentos: List<DepartamentosEnt>
    lateinit var lDepCat: List<DepartParaCat>


    fun abrir(fGrupo: Short): Boolean {
        lDepartamentos = departamentosDao?.getGrupo(fGrupo) ?: emptyList<DepartamentosEnt>().toMutableList()
        return (lDepartamentos.count() > 0)
    }


    fun abrirParaCatalogo(fGrupo: Short): Boolean {
        lDepCat = departamentosDao?.abrirParaCatalogo(fGrupo) ?: emptyList<DepartParaCat>().toMutableList()
        return (lDepCat.count() > 0)
    }


    //fun getImagen(): String {
    //    return "DPT_" + ponerCeros(getGrupo().toString(), ancho_grupo) +
    //            ponerCeros(getCodigo().toString(), ancho_departamento) + ".jpg"
    //}

}