package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * Created by jabegines on 26/11/13.
 */
class HcoArtCliente(contexto: Context) {
    lateinit var cHcoArtClte: Cursor


    fun abrir(QueCliente: Int, QueArticulo: Int) {
        // TODO
        /*
        cHcoArtClte = dbAlba.rawQuery(
            "SELECT A._id, A.precio, A.precioii, A.cantidad, A.dto,"
                    + " B.tipodoc, B.serie, B.numero, B.fecha, C.iva porciva FROM Lineas A"
                    + " LEFT JOIN cabeceras B ON B._id = A.cabeceraId"
                    + " LEFT JOIN ivas C ON C.codigo = A.codigoiva"
                    + " WHERE A.articulo = " + QueArticulo + " AND B.cliente = " + QueCliente
                    + " ORDER BY substr(B.fecha, 7)||substr(B.fecha, 4, 2)||substr(B.fecha, 1, 2) DESC",
            null
        )
        cHcoArtClte.moveToFirst()
        */
    }

}