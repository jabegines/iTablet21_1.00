package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor


/**
 * Created by jabegines on 13/06/2014.
 */
class Clasificadores(contexto: Context) {

    lateinit var cursor: Cursor


    fun close() {
        if (this::cursor.isInitialized)
            cursor.close()
    }

    // En la tabla de clasificadores el flag 0 nos indicará que el registro pertenece a la clasificación avanzada
    // y el flag 1 nos indicará que el registro es un catálogo.
    fun abrir(quePadre: Int, queNivel: Int): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery(
            "SELECT * FROM clasificadores WHERE padre = " + quePadre + " AND nivel = " + queNivel
                    + " AND Flag = 0 ORDER BY orden", null
        )
        return cursor.moveToFirst()
        */
        return true
    }

    fun abrirCatalogos(): Boolean {
        // TODO
        //cursor = dbAlba.rawQuery(
        //    "SELECT codigo _id, codigo, descr, padre FROM clasificadores WHERE Flag = 1 ORDER BY orden",
        //    null
        //)
        //return cursor.moveToFirst()
        return true
    }

    fun abrirBioCatalogo(): Boolean {
        //  TODO
        /*
        cursor = dbAlba.rawQuery(
            "SELECT A.codigo _id, A.descr," +
                    " (SELECT COUNT(*) FROM articclasif WHERE clasificador = A.codigo) numarticulos" +
                    " FROM clasificadores A" +
                    " WHERE A.Flag = 1" +
                    " ORDER BY orden", null
        )
        return cursor.moveToFirst()
        */
        return true
    }

    fun getCodigo(): Int {
        return cursor.getInt(cursor.getColumnIndex("codigo") ?: 0)
    }

    fun getDescripcion(): String {
        return cursor.getString(cursor.getColumnIndex("descr") ?: 1)
    }

    val imagen: String
        get() = "CLS_" + getCodigo() + ".jpg"

}