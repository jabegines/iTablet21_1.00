package es.albainformatica.albamobileandroid.reparto

import es.albainformatica.albamobileandroid.BaseDatos
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class Reparto(contexto: Context): BaseDatos(contexto) {
    private var dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cursorDocs: Cursor
    val idDocumento: Int
        get() = cursorDocs.getInt(cursorDocs.getColumnIndex("_id"))


    override fun close() {
        dbAlba.close()
    }

    fun abrir(fRuta: String): Boolean {
        val cCadena: String = if (fRuta != "")
            "SELECT DISTINCT B._id, A.cliente, B.tipodoc, (B.serie || '/' || B.numero) AS serienumero," +
                    " B.fecha, A.codigo, A.nomfi, A.nomco, 0 tienepend," +  //" B.fecha, A.codigo, A.nomfi, A.nomco, ifnull(C.cliente, 0) tienepend," +
                    " B.estado, B.firmado, B.tipoincidencia FROM Clientes A" +
                    " LEFT JOIN cabeceras B ON B.cliente = A.cliente" +  //" LEFT JOIN pendiente C ON C.cliente = A.cliente AND C.estado <> 'L' AND C.tipodoc <> 2" +
                    //" WHERE B.hoja = " + fRuta + " OR C.hoja = " + fRuta +
                    " WHERE B.hoja = " + fRuta +
                    " ORDER BY B.orden"
        else
            "SELECT DISTINCT B._id, A.cliente, B.tipodoc, (B.serie || '/' || B.numero) AS serienumero," +
                    " B.fecha, A.codigo, A.nomfi, A.nomco, 0 tienepend," +  //" B.fecha, A.codigo, A.nomfi, A.nomco, ifnull(C.cliente, 0) tienepend," +
                    " B.estado, B.firmado, B.tipoincidencia FROM Clientes A" +
                    " LEFT JOIN cabeceras B ON B.cliente = A.cliente" +  //" LEFT JOIN pendiente C ON C.cliente = A.cliente AND C.estado <> 'L' AND C.tipodoc <> 2" +
                    //" WHERE B.hoja = " + fRuta + " OR C.hoja = " + fRuta +
                    " ORDER BY B.orden"

        cursorDocs = dbAlba.rawQuery(cCadena, null)
        return cursorDocs.moveToFirst()
    }

    fun situarEnDocumento(anteriorDoc: Int, irASiguDoc: Boolean): Boolean {
        if (anteriorDoc > 0) {
            try {
                //for (cursorDocs.moveToFirst(); cursorDocs.getInt(cursorDocs.getColumnIndex("_id")) != anteriorDoc; cursorDocs.moveToNext())
                do {
                    cursorDocs.moveToNext()
                } while (cursorDocs.getInt(cursorDocs.getColumnIndex("_id")) != anteriorDoc)
            } catch (e: Exception) {
                Log.e("Excepcion", e.message ?: "")
            }
            if (irASiguDoc) cursorDocs.moveToNext()
        } else cursorDocs.moveToFirst()
        return !cursorDocs.isAfterLast
    }

    fun marcarComoEntregado(queIdDocumento: Int, fRuta: String) {
        // Obtenemos la fecha y hora actuales, que son las que grabaremos como fecha y hora de la firma.
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val values = ContentValues()
        values.put("firmado", "T")
        values.put("fechafirma", df.format(tim))
        values.put("horafirma", dfHora.format(tim))
        dbAlba.update("cabeceras", values, "_id=$queIdDocumento", null)

        // Refresco el cursor cerrándolo y volviéndolo a abrir.
        cursorDocs.close()
        abrir(fRuta)
    }

    fun setTextoIncidencia(queIdDocumento: Int, queTexto: String, fRuta: String, queTipoIncid: Int) {
        val values = ContentValues()
        values.put("tipoincidencia", queTipoIncid)
        values.put("textoincidencia", queTexto)
        dbAlba.update("cabeceras", values, "_id=$queIdDocumento", null)

        // Refresco el cursor cerrándolo y volviéndolo a abrir.
        cursorDocs.close()
        abrir(fRuta)
    }


}