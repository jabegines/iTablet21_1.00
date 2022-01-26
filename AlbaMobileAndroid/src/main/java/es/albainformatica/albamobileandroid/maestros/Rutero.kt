package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.DatosRutero
import es.albainformatica.albamobileandroid.dao.RuterosDao
import es.albainformatica.albamobileandroid.database.MyDatabase

/**
 * Created by jabegines on 14/10/13.
 */
class Rutero(contexto: Context) {
    private val ruterosDao: RuterosDao? = MyDatabase.getInstance(contexto)?.ruterosDao()
    private val fConfiguracion: Configuracion = Comunicador.fConfiguracion

    lateinit var lRutero: List<DatosRutero>

    var cursor: Cursor? = null



    fun abrirRuta(fRuta: Short): Boolean {
        lRutero = ruterosDao?.abrirRuta(fRuta) ?: emptyList<DatosRutero>().toMutableList()

        return (lRutero.count() > 0)
    }

    /*
    fun abrirParaReparto(fRuta: String): Boolean {
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
    */


    fun abrirCodPostal(queCodPostal: String): Boolean {
        lRutero = if (fConfiguracion.aconsNomComercial())
            ruterosDao?.abrirCodPostal(queCodPostal, 2) ?: emptyList<DatosRutero>().toMutableList()
        else
            ruterosDao?.abrirCodPostal(queCodPostal, 1) ?: emptyList<DatosRutero>().toMutableList()

        return (lRutero.count() > 0)
    }


    /*
    fun situarEnCliente(anteriorClte: Int, irASiguClte: Boolean): Boolean {
        if (anteriorClte > 0) {
            try {
                for (datosRutero in lRutero) {
                    if (datosRutero.clienteId == anteriorClte)
                        break
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
    */

}