package es.albainformatica.albamobileandroid

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase

/**
 * Created by jabegines on 10/10/13.
 */
open class BaseDatos(contexto: Context): SQLiteOpenHelper(contexto, queBaseDatos, null, VERSION_BD) {

    companion object {
        var queBaseDatos: String = ""
    }


    override fun onCreate(db: SQLiteDatabase?) {
        // Se ejecuta la sentencia SQL de creación de la tabla
    }

    override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
        if (versionAnterior < 2) cambiosVersion2(db)
        if (versionAnterior < 3) cambiosVersion3(db)
        if (versionAnterior < 4) cambiosVersion4(db)
    }

    private fun cambiosVersion2(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS ofertas")
        db.execSQL("DROP TABLE IF EXISTS oftVolumen")
        db.execSQL("DROP TABLE IF EXISTS oftVolRangos")
        db.execSQL("DROP TABLE IF EXISTS oftCantRangos")
        db.execSQL("ALTER TABLE cabeceras ADD COLUMN imprimido VARCHAR(1)")
    }

    private fun cambiosVersion3(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS divisas")
        db.execSQL("DROP TABLE IF EXISTS cobros")
    }

    private fun cambiosVersion4(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS rutas")
        db.execSQL("DROP TABLE IF EXISTS notasclientes")
        db.execSQL("DROP TABLE IF EXISTS tiposincid")
        db.execSQL("DROP TABLE IF EXISTS tempClientes")
        db.execSQL("DROP TABLE IF EXISTS numexport")
        db.execSQL("DROP TABLE IF EXISTS histrepre")
        db.execSQL("DROP TABLE IF EXISTS articdatadic")
        db.execSQL("DROP TABLE IF EXISTS ratingprov")
        db.execSQL("DROP TABLE IF EXISTS catalogoLineas")
        db.execSQL("DROP TABLE IF EXISTS cabDiferidas")
        db.execSQL("DROP TABLE IF EXISTS lineasDiferidas")
        db.execSQL("DROP TABLE IF EXISTS conclientes")
        db.execSQL("DROP TABLE IF EXISTS proveedores")
        db.execSQL("DROP TABLE IF EXISTS ratinggrupos")
    }

}