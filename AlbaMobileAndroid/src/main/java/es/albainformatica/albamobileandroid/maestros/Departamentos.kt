package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.ponerCeros
import android.database.sqlite.SQLiteDatabase
import es.albainformatica.albamobileandroid.ancho_departamento
import es.albainformatica.albamobileandroid.ancho_grupo

/**
 * Created by jabegines on 06/05/2014.
 */
class Departamentos(contexto: Context) : BaseDatos(contexto) {
    private val dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cursor: Cursor


    override fun close() {
        if (this::cursor.isInitialized)
            cursor.close()
        dbAlba.close()
    }

    fun abrir(fGrupo: Int): Boolean {
        cursor = dbAlba.rawQuery("SELECT * FROM departamentos WHERE grupo = $fGrupo", null)
        return cursor.moveToFirst()
    }

    fun abrirParaCatalogo(fGrupo: Int): Boolean {
        cursor = dbAlba.rawQuery(
            "SELECT A.codigo _id, A.descr," +
                    " (SELECT COUNT(*) FROM articulos WHERE grupo = " + fGrupo + " AND dpto = A.codigo) numarticulos" +
                    " FROM departamentos A" +
                    " WHERE A.grupo = " + fGrupo +
                    " ORDER BY A.codigo", null
        )
        return cursor.moveToFirst()
    }

    fun getCodigo(): Int {
        return cursor.getInt(cursor.getColumnIndex("codigo") ?: 1)
    }


    fun getGrupo(): Int {
        return cursor.getInt(cursor.getColumnIndex("grupo") ?: 0)
    }

    fun getDescripcion(): String {
        return cursor.getString(cursor.getColumnIndex("descr") ?: 2)
    }

    fun getImagen(): String {
        return "DPT_" + ponerCeros(getGrupo().toString(), ancho_grupo) +
                ponerCeros(getCodigo().toString(), ancho_departamento) + ".jpg"
    }

}