package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.dao.DepartamentosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DepartamentosEnt

/**
 * Created by jabegines on 06/05/2014.
 */
class Departamentos(contexto: Context) {
    private val departamentosDao: DepartamentosDao? = MyDatabase.getInstance(contexto)?.departamentosDao()

    lateinit var lDepartamentos: List<DepartamentosEnt>



    fun abrir(fGrupo: Short): Boolean {
        lDepartamentos = departamentosDao?.getGrupo(fGrupo) ?: emptyList<DepartamentosEnt>().toMutableList()
        return (lDepartamentos.count() > 0)
    }


    fun abrirParaCatalogo(fGrupo: Int): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery(
            "SELECT A.codigo _id, A.descr," +
                    " (SELECT COUNT(*) FROM articulos WHERE grupo = " + fGrupo + " AND dpto = A.codigo) numarticulos" +
                    " FROM departamentos A" +
                    " WHERE A.grupo = " + fGrupo +
                    " ORDER BY A.codigo", null
        )
        return cursor.moveToFirst()
        */
        return true
    }


    //fun getImagen(): String {
    //    return "DPT_" + ponerCeros(getGrupo().toString(), ancho_grupo) +
    //            ponerCeros(getCodigo().toString(), ancho_departamento) + ".jpg"
    //}

}