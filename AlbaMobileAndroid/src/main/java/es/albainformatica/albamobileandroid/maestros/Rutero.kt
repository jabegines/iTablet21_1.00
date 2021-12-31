package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.BaseDatos
import es.albainformatica.albamobileandroid.Configuracion
import android.util.Log
import es.albainformatica.albamobileandroid.Comunicador
import java.lang.Exception

/**
 * Created by jabegines on 14/10/13.
 */
class Rutero(contexto: Context): BaseDatos(contexto) {
    var cursor: Cursor? = null
    private val fConfiguracion: Configuracion = Comunicador.fConfiguracion


    fun abrirRuta(fRuta: String): Boolean {
        val dbAlba = readableDatabase
        cursor = dbAlba.rawQuery(
            "SELECT A._id, A.orden, A.cliente, B.codigo, B.nomfi, B.nomco, B.tieneincid FROM rutero A" +
                    " JOIN clientes B ON B.cliente = A.cliente" +
                    " WHERE A.ruta = " + fRuta +
                    " ORDER BY A.orden", null
        )
        return cursor?.moveToFirst() ?: false
    }

    fun abrirParaReparto(fRuta: String): Boolean {
        val dbAlba = readableDatabase
        cursor = dbAlba.rawQuery(
            "SELECT DISTINCT A._id, A.orden, A.cliente, B.codigo, B.nomfi, B.nomco, B.tieneincid," +
                    " ifnull(C.cliente, 0) tienedocumentos, FROM rutero A" +  //" ifnull(C.cliente, 0) tienedocumentos, ifnull(D.cliente, 0) tienepend FROM rutero A" +
                    " JOIN clientes B ON B.cliente = A.cliente" +
                    " LEFT JOIN cabeceras C ON C.cliente = B.cliente AND C.firmado <> 'T' AND C.estado = '0'" +  //" LEFT JOIN pendiente D ON D.cliente = B.cliente AND D.estado <> 'L'" +
                    " WHERE A.ruta = " + fRuta +
                    " ORDER BY A.orden", null
        )
        return cursor?.moveToFirst() ?: false
    }

    fun abrirCodPostal(queCodPostal: String): Boolean {
        val dbAlba = readableDatabase
        val consulta: String = if (fConfiguracion.aconsNomComercial()) "SELECT cliente _id, 1 orden, cliente, codigo," +
                    " nomfi, nomco, tieneincid FROM clientes WHERE cpostal = '" + queCodPostal + "'" +
                    " ORDER BY nomco" else "SELECT cliente _id, 1 orden, cliente, codigo," +
                    " nomfi, nomco, tieneincid FROM clientes WHERE cpostal = '" + queCodPostal + "'" +
                    " ORDER BY nomfi"
        cursor = dbAlba.rawQuery(consulta, null)
        return cursor?.moveToFirst() ?: false
    }

    fun situarEnCliente(anteriorClte: Int, irASiguClte: Boolean): Boolean {
        if (anteriorClte > 0) {
            try {
                cursor?.moveToFirst()
                while (cursor?.getInt(cursor?.getColumnIndex("cliente") ?: 0) != anteriorClte) {
                    cursor?.moveToNext()
                }
            } catch (e: Exception) {
                Log.e("Excepcion", e.message ?: "")
            }
            if (irASiguClte) cursor?.moveToNext()
        } else cursor?.moveToFirst()

        return if (cursor != null)
            !cursor!!.isAfterLast
        else
            false
    }

    val cliente: Int
        get() = cursor?.getInt(cursor?.getColumnIndex("cliente") ?: 0) ?: 0

}