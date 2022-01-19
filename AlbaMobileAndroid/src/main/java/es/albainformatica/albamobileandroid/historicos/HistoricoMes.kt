package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.dao.HistMesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.HistMesEnt
import java.util.*


class HistoricoMes(val contexto: Context) {
    var histMesDao: HistMesDao? = MyDatabase.getInstance(contexto)?.histMesDao()

    lateinit var cCursorHco: Cursor

    lateinit var lDatosHistMes: List<HistMesEnt>


    fun Abrir(queCliente: Int) {
        // Obtenemos el mes de la fecha actual.
        val calendarNow: Calendar = GregorianCalendar()
        val month = calendarNow[Calendar.MONTH] + 1
        val sql =
            "SELECT A._id, A.articulo, B.codigo, B.descr, A.cantidadant, A.cantidad, (A.cantidad - A.cantidadant) diferencia, A.mes FROM histmes A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " WHERE A.cliente = " + queCliente + " AND mes = " + month +
                    " ORDER BY B.codigo"
        // TODO
        //cCursorHco = dbAlba.rawQuery(sql, null)
        //cCursorHco.moveToFirst()
    }


    fun abrirArticulo(queArticulo: Int, queCliente: Int): Boolean {
        // TODO
        /*
        cCursorHco = dbAlba.rawQuery(
            "SELECT * FROM histmes WHERE articulo = $queArticulo AND cliente = $queCliente",
            null
        )
        return cCursorHco.moveToFirst()
         */
        return true
    }


    fun abrirCliente(queCliente: Int): Boolean {
        // TODO
        //cCursorHco = dbAlba.rawQuery("SELECT * FROM histmes WHERE cliente = $queCliente", null)
        //return cCursorHco.moveToFirst()
        return true
    }


    fun abrirClteArt(queCliente: Int, queArticulo: Int) {
        // Obtenemos el mes de la fecha actual.
        val calendarNow: Calendar = GregorianCalendar()
        val month = calendarNow[Calendar.MONTH] + 1

        lDatosHistMes = histMesDao?.abrirClteArt(queCliente, queArticulo, month) ?: emptyList<HistMesEnt>().toMutableList()
    }


    fun AbrirAnyo(queCliente: Int, queArticulo: Int) {
        val sql = "SELECT A.articulo, B.codigo, B.descr, A.cantidad, A.mes FROM histmes A" +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                " WHERE A.cliente = " + queCliente + " AND A.articulo = " + queArticulo +
                " ORDER BY A.mes"
        // TODO
        //cCursorHco = dbAlba.rawQuery(sql, null)
        //cCursorHco.moveToFirst()
    }


    fun abrirHcoClte(queCliente: Int) {
        val sql =
            "SELECT A._id, A.cliente, A.articulo, B.codigo, B.descr, SUM(A.cantidad) sumCant, SUM(A.cantidadant) sumCantAnt," +
                    " SUM(A.importe) sumImpte, SUM(A.importeant) sumImpteAnt FROM histmes A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " WHERE A.cliente = " + queCliente +
                    " GROUP BY A.cliente, A.articulo" +
                    " ORDER BY B.codigo"
        // TODO
        //cCursorHco = dbAlba.rawQuery(sql, null)
        //cCursorHco.moveToFirst()
    }


    fun totalesHcoClte(queCliente: Int): Boolean {
        val sql = "SELECT SUM(A.cantidad) sumCant, SUM(A.cantidadant) sumCantAnt," +
                " SUM(A.importe) sumImpte, SUM(A.importeant) sumImpteAnt FROM histmes A" +
                " WHERE A.cliente = " + queCliente +
                " GROUP BY A.cliente"
        // TODO
        //cCursorHco = dbAlba.rawQuery(sql, null)
        //return cCursorHco.moveToFirst()
        return true
    }


    fun getCantidad2Int(): Int {
        return if (cCursorHco.count > 0) {
            val sCantidad = cCursorHco.getString(cCursorHco.getColumnIndex("cantidad")).replace(',', '.')
            val dCantidad = sCantidad.toDouble()
            val L = Math.round(dCantidad)
            Integer.valueOf(L.toInt())
        } else 0
    }



}