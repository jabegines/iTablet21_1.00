package es.albainformatica.albamobileandroid.comunicaciones

import android.content.Context
import es.albainformatica.albamobileandroid.dao.NumExportDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.NumExportEnt
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 10/12/13.
 */
class NumExportaciones(contexto: Context) {
    private val numExpDao: NumExportDao? = MyDatabase.getInstance(contexto)?.numExportDao()


    fun abrir(): MutableList<NumExportEnt> {

        return numExpDao?.getAllExport() ?: emptyList<NumExportEnt>().toMutableList()
    }


    fun guardarExportacion(iSigExportacion: Int) {
        val numExpEnt = NumExportEnt()

        numExpEnt.numExport = iSigExportacion

        // Obtenemos la fecha actual.
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fFecha = df.format(tim)
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fHora = dfHora.format(tim)

        numExpEnt.fecha = fFecha
        numExpEnt.hora = fHora
        numExpDao?.insertar(numExpEnt)
    }

}