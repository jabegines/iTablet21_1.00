package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * Created by jabegines on 05/05/2014.
 */
class Grupos(contexto: Context) {

    lateinit var cursor: Cursor

    fun close() {
        cursor.close()
    }

    fun abrir(): Boolean {
        // TODO
        //cursor = dbAlba.rawQuery("SELECT * FROM grupos", null)
        //return cursor.moveToFirst()
        return true
    }

    fun abrirParaCatalogo(): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery(
            "SELECT A.codigo _id, A.descr," +
                    " (SELECT COUNT(*) FROM departamentos WHERE grupo = A.codigo) numdepartamentos" +
                    " FROM grupos A" +
                    " WHERE numdepartamentos > 0", null
        )
        return cursor.moveToFirst()
        */
        return true
    }

    val codigo: Int
        get() = cursor.getInt(cursor.getColumnIndex("codigo"))
    val descripcion: String
        get() = cursor.getString(cursor.getColumnIndex("descr"))
    val imagen: String
        get() = "GRP_$codigo.jpg"

}