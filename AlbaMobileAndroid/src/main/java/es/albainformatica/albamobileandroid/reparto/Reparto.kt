package es.albainformatica.albamobileandroid.reparto

import android.content.Context
import es.albainformatica.albamobileandroid.DatosReparto
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.ClientesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import java.text.SimpleDateFormat
import java.util.*

class Reparto(contexto: Context) {
    private var clientesDao: ClientesDao? = MyDatabase.getInstance(contexto)?.clientesDao()
    private var cabecerasDao: CabecerasDao? = MyDatabase.getInstance(contexto)?.cabecerasDao()

    lateinit var lDocsReparto: List<DatosReparto>



    fun abrir(fRuta: Short): Boolean {
        lDocsReparto = if (fRuta > 0)
            clientesDao?.getDocsReparto(fRuta) ?: emptyList<DatosReparto>().toMutableList()
        else
            emptyList<DatosReparto>().toMutableList()

        return (lDocsReparto.isNotEmpty())
    }

    fun buscarRutaActiva(): Short {
        return cabecerasDao?.buscarRutaActiva()?.toShort() ?: 0
    }


    fun marcarComoEntregado(queIdDocumento: Int) {
        // Obtenemos la fecha y hora actuales, que son las que grabaremos como fecha y hora de la firma.
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        cabecerasDao?.marcarComoEntregado(queIdDocumento, df.format(tim), dfHora.format(tim))
    }


    fun setTextoIncidencia(queIdDocumento: Int, queTexto: String, queTipoIncid: Int) {
        cabecerasDao?.setTextoIncidencia(queIdDocumento, queTipoIncid, queTexto)
    }


}