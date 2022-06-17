package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import es.albainformatica.albamobileandroid.DatosHistMesAnyo
import es.albainformatica.albamobileandroid.DatosHistMesClte
import es.albainformatica.albamobileandroid.DatosHistMesDif
import es.albainformatica.albamobileandroid.TotalesHistMes
import es.albainformatica.albamobileandroid.dao.HistMesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.HistMesEnt
import java.util.*


class HistoricoMes(val contexto: Context) {
    private var histMesDao: HistMesDao? = MyDatabase.getInstance(contexto)?.histMesDao()

    lateinit var lDatosHistMes: List<HistMesEnt>
    lateinit var lDatosHMAnyo: List<DatosHistMesAnyo>
    lateinit var lDatosHMDif: List<DatosHistMesDif>
    lateinit var lDatosHcoMesClte: List<DatosHistMesClte>
    lateinit var totalesHistMes: TotalesHistMes


    fun abrir(queCliente: Int) {
        // Obtenemos el mes de la fecha actual.
        val calendarNow: Calendar = GregorianCalendar()
        val month = calendarNow[Calendar.MONTH] + 1

        lDatosHMDif = histMesDao?.abrir(queCliente, month) ?: emptyList<DatosHistMesDif>().toMutableList()
    }


    fun abrirArticulo(queArticulo: Int, queCliente: Int): Boolean {
        lDatosHistMes = histMesDao?.abrirArticulo(queArticulo, queCliente) ?: emptyList<HistMesEnt>().toMutableList()
        return (lDatosHistMes.isNotEmpty())
    }


    fun abrirCliente(queCliente: Int): Boolean {
        lDatosHistMes = histMesDao?.abrirCliente(queCliente) ?: emptyList<HistMesEnt>().toMutableList()
        return (lDatosHistMes.isNotEmpty())
    }


    fun abrirClteArt(queCliente: Int, queArticulo: Int) {
        // Obtenemos el mes de la fecha actual.
        val calendarNow: Calendar = GregorianCalendar()
        val month = calendarNow[Calendar.MONTH] + 1

        lDatosHistMes = histMesDao?.abrirClteArt(queCliente, queArticulo, month) ?: emptyList<HistMesEnt>().toMutableList()
    }


    fun abrirAnyo(queCliente: Int, queArticulo: Int) {
        lDatosHMAnyo = histMesDao?.abrirAnyo(queCliente, queArticulo) ?: emptyList<DatosHistMesAnyo>().toMutableList()
    }


    fun abrirHcoClte(queCliente: Int) {
        lDatosHcoMesClte = histMesDao?.abrirHcoClte(queCliente) ?: emptyList<DatosHistMesClte>().toMutableList()
    }


    fun totalesHcoClte(queCliente: Int): Boolean {
        totalesHistMes = histMesDao?.totalesHcoClte(queCliente) ?: TotalesHistMes()

        return (totalesHistMes.sumCant != "" || totalesHistMes.sumCantAnt != ""
                || totalesHistMes.sumImpte != "" || totalesHistMes.sumImpteAnt != "")
    }




}