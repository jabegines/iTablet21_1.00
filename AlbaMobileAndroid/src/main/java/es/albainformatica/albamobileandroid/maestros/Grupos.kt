package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.BaseDatos
import android.database.sqlite.SQLiteDatabase

/**
 * Created by jabegines on 05/05/2014.
 */
class Grupos(contexto: Context) : BaseDatos(contexto) {
    private var dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cursor: Cursor

    override fun close() {
        cursor.close()
        dbAlba.close()
    }

    fun abrir(): Boolean {
        cursor = dbAlba.rawQuery("SELECT * FROM grupos", null)
        return cursor.moveToFirst()
    }

    fun abrirParaCatalogo(): Boolean {
        cursor = dbAlba.rawQuery(
            "SELECT A.codigo _id, A.descr," +
                    " (SELECT COUNT(*) FROM departamentos WHERE grupo = A.codigo) numdepartamentos" +
                    " FROM grupos A" +
                    " WHERE numdepartamentos > 0", null
        )
        return cursor.moveToFirst()
    }

    val codigo: Int
        get() = cursor.getInt(cursor.getColumnIndex("codigo"))
    val descripcion: String
        get() = cursor.getString(cursor.getColumnIndex("descr"))
    val imagen: String
        get() = "GRP_$codigo.jpg"

}