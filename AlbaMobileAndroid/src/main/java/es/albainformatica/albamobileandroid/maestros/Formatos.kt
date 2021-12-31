package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase


class Formatos(val contexto: Context): BaseDatos(contexto) {
    private var dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cursor: Cursor


    override fun close() {
        this.dbAlba.close()
        super.close()
    }


    fun abrirFormatos(queArticulo: Int, queCliente: Int): Boolean {
        cursor = dbAlba.rawQuery("SELECT DISTINCT A.codigo, A.descr, C._id linFtoLin, C.borrar, D._id hcoId FROM formatos A" +
                " JOIN trfformatos B ON B.formato = A.codigo AND B.articulo = " + queArticulo +
                " LEFT JOIN ftosLineas C ON C.articulo = " + queArticulo + " AND C.formato = A.codigo" +
                " LEFT JOIN historico D ON D.articulo = " + queArticulo + " AND D.cliente = " + queCliente + " AND D.formato = A.codigo" +
                " ORDER BY A.codigo", null)

        return cursor.moveToFirst()
    }


    fun todosLosFormatos(): Boolean {
        cursor = dbAlba.rawQuery("SELECT codigo, descr, dosis1 FROM formatos", null);
        return cursor.moveToFirst();
    }


}