package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor


class Formatos(val contexto: Context) {

    lateinit var cursor: Cursor



    fun abrirFormatos(queArticulo: Int, queCliente: Int): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery("SELECT DISTINCT A.codigo, A.descr, C._id linFtoLin, C.borrar, D._id hcoId FROM formatos A" +
                " JOIN trfformatos B ON B.formato = A.codigo AND B.articulo = " + queArticulo +
                " LEFT JOIN ftosLineas C ON C.articulo = " + queArticulo + " AND C.formato = A.codigo" +
                " LEFT JOIN historico D ON D.articulo = " + queArticulo + " AND D.cliente = " + queCliente + " AND D.formato = A.codigo" +
                " ORDER BY A.codigo", null)

        return cursor.moveToFirst()
        */
        return true
    }


    fun todosLosFormatos(): Boolean {
        // TODO
        //cursor = dbAlba.rawQuery("SELECT codigo, descr, dosis1 FROM formatos", null);
        //return cursor.moveToFirst();
        return true
    }


}