package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.BaseDatos
import android.database.sqlite.SQLiteDatabase

class HcoComSemMes(contexto: Context) : BaseDatos(contexto) {
    private val dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cCursorHco: Cursor


    fun abrir(queCliente: Int, sHoy: String, sHoyMenos6: String, sHoyMenos7: String, sHoyMenos13: String) {
        val sql =
            "SELECT codigo AS _id, SUM(suma1) AS suma1, SUM(suma2) AS suma2, codigo, descr FROM(" +
                    " SELECT SUM(A.cantidad) AS suma1, 0 AS suma2, B.codigo, B.descr FROM hcoCompSemMes A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " WHERE A.cliente = " + queCliente +
                    " AND julianday(A.fecha) >= julianday('" + fechaEnJulian(sHoyMenos6) + "')" +
                    " AND julianday(A.fecha) <= julianday('" + fechaEnJulian(sHoy) + "')" +
                    " GROUP BY B.codigo, B.descr" +
                    " UNION ALL" +
                    " SELECT 0 AS suma1, SUM(A.cantidad) AS suma2, B.codigo, B.descr FROM hcoCompSemMes A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " WHERE A.cliente = " + queCliente +
                    " AND julianday(A.fecha) >= julianday('" + fechaEnJulian(sHoyMenos13) + "')" +
                    " AND julianday(A.fecha) <= julianday('" + fechaEnJulian(sHoyMenos7) + "')" +
                    " GROUP BY B.codigo, B.descr" +
                    " ) AS consulta GROUP BY codigo"
        cCursorHco = dbAlba.rawQuery(sql, null)
        cCursorHco.moveToFirst()
    }

    private fun fechaEnJulian(queFecha: String): String {
        val queAnyo = queFecha.substring(6, 10)
        val queMes = queFecha.substring(3, 5)
        val queDia = queFecha.substring(0, 2)
        return "$queAnyo-$queMes-$queDia"
    }

}