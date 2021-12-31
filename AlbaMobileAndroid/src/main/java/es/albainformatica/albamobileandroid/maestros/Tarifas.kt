package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.ArrayList

/**
 * Created by jabegines on 11/10/13.
 */
class Tarifas(contexto: Context): BaseDatos(contexto) {
    private lateinit var dbAlba: SQLiteDatabase
    private lateinit var cursor: Cursor

    fun abrir(): Boolean {
        dbAlba = readableDatabase
        cursor = dbAlba.rawQuery("SELECT * FROM cnftarifas ORDER by codigo", null)
        return cursor.moveToFirst()
    }

    private val codigo: String
        get() {
            val columna = cursor.getColumnIndex("codigo")
            return cursor.getString(columna)
        }
    private val descripcion: String
        get() {
            val columna = cursor.getColumnIndex("tarifa")
            return cursor.getString(columna)
        }

    fun llenarArray(sArrayList: ArrayList<String>) {
        sArrayList.add("Sin tarifa")
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            sArrayList.add("$codigo $descripcion")
            cursor.moveToNext()
        }
    }
}