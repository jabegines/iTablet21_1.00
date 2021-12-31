package es.albainformatica.albamobileandroid.historicos

import es.albainformatica.albamobileandroid.BaseDatos
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * Created by jabegines on 14/10/13.
 */
class Historico(contexto: Context) : BaseDatos(contexto) {
    private val dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cHco: Cursor

    private var fCliente = 0
    private var fCadBusqueda = ""
    var fArticulo = 0
    var fCodigo: String = ""
    var fDescr: String = ""
    var fCajas: Double = 0.0
    var fCantidad: Double = 0.0
    var fPiezas: Double = 0.0
    private var fLineaPorPiezas = false
    var fPrecio: Double = 0.0
    var fPrecioII: Double = 0.0
    var fDtoLin: Double = 0.0
    var fDtoImp: Double = 0.0
    var fDtoImpII: Double = 0.0
    var fCodigoIva: Short = 0
    var fTasa1: Double = 0.0
    var fTasa2: Double = 0.0
    var fFormatoLin: Byte = 0
    var fHayArtHabituales: Boolean = false
    var fTextoLinea: String = ""
    var fLote: String = ""
    var fFlag = 0
    var fFlag3 = 0
    var fFlag5 = 0
    var fAlmacPedido: String = "0"
    var fIncidencia = 0


    fun abrir(QueCliente: Int) {
        fCliente = QueCliente
        var sql = "SELECT A.*, B.codigo, B.descr, C.cantidad cantpedida, D.iva porciva," +
                " (E.ent - E.sal) stock, (F.descr) descrfto FROM historico A" +
                " LEFT OUTER JOIN articulos B ON B.articulo = A.articulo" +
                " LEFT OUTER JOIN tmphco C ON C.linea = A._id" +
                " LEFT OUTER JOIN ivas D ON D.tipo = B.tipoiva" +
                " LEFT OUTER JOIN stock E ON E.articulo = A.articulo" +
                " LEFT OUTER JOIN formatos F ON F.codigo = A.formato" +
                " WHERE A.cliente = " + QueCliente
        if (fCadBusqueda != "") sql = "$sql AND B.descr LIKE('%$fCadBusqueda%')"
        sql = "$sql ORDER BY B.descr"
        cHco = dbAlba.rawQuery(sql, null)
        cHco.moveToFirst()
    }

    fun abrirConBusqueda(QueCliente: Int, artBuscar: String) {
        fCadBusqueda = artBuscar
        if (this::cHco.isInitialized)
            cHco.close()

        cHco = dbAlba.rawQuery(
            "SELECT A._id, A.articulo, A.formato, A.cantidad, A.precio, A.dto, A.precio AS prneto, A.cajas, A.fecha," +
                    " B.codigo, B.descr, C.piezas piezpedida, C.cantidad cantpedida, D.iva porciva," +
                    " (E.ent - E.sal) stock, (F.descr) descrfto, G.texto FROM historico A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " LEFT JOIN tmphco C ON C.linea = A._id" +
                    " LEFT JOIN ivas D ON D.tipo = B.tipoiva" +
                    " LEFT JOIN stock E ON E.articulo = A.articulo" +
                    " LEFT JOIN formatos F ON F.codigo = A.formato" +
                    " LEFT JOIN arthabituales G ON G.articulo = A.articulo AND G.formato = A.formato AND G.cliente = " + QueCliente +
                    " WHERE A.cliente = " + QueCliente + " AND B.descr LIKE('%" + artBuscar + "%')" +
                    " ORDER BY B.descr", null
        )
        cHco.moveToFirst()
    }

    fun abrirHcoPorArtClte(queCliente: Int, queOrdenacion: Short) {
        if (this::cHco.isInitialized)
            cHco.close()

        val queCadena: String
        queCadena = if (queOrdenacion.toInt() == 0) {
            "SELECT A._id, A.articulo, B.codigo, B.descr, C.cantidad cantpedida, D.porcDevol FROM hcoPorArticClte A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " LEFT JOIN tmphco C ON C.linea = A._id" +
                    " LEFT JOIN estadDevoluc D ON D.cliente = " + queCliente + " AND D.articulo = A.articulo" +
                    " WHERE A.cliente = " + queCliente +
                    " GROUP BY A.articulo" +
                    " ORDER BY B.descr"
        } else {
            "SELECT A._id, A.articulo, B.codigo, B.descr, C.cantidad cantpedida, D.porcDevol FROM hcoPorArticClte A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " LEFT JOIN tmphco C ON C.linea = A._id" +
                    " LEFT JOIN estadDevoluc D ON D.cliente = " + queCliente + " AND D.articulo = A.articulo" +
                    " WHERE A.cliente = " + queCliente +
                    " GROUP BY A.articulo" +
                    " ORDER BY B.codigo"
        }
        cHco = dbAlba.rawQuery(queCadena, null)
        cHco.moveToFirst()
    }

    fun inicializarLinea() {
        fArticulo = 0
        fCodigo = ""
        fDescr = ""
        fCajas = 0.0
        fCantidad = 0.0
        fPiezas = 0.0
        fPrecio = 0.0
        fPrecioII = 0.0
        fDtoLin = 0.0
        fDtoImp = 0.0
        fDtoImpII = 0.0
        fTasa1 = 0.0
        fTasa2 = 0.0
        fFormatoLin = 0
        fLote = ""
        fFlag = 0
        fFlag3 = 0
        fFlag5 = 0
        fAlmacPedido = ""
        fIncidencia = 0
        fLineaPorPiezas = false
    }

    fun aceptarCambios(fLinea: Int) {
        var fInsertando = true
        dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
            cTmpHco.moveToFirst()
            while (!cTmpHco.isAfterLast) {
                if (cTmpHco.getInt(cTmpHco.getColumnIndex("linea") ?: 0) == fLinea) fInsertando = false
                cTmpHco.moveToNext()
            }
            val values = ContentValues()
            values.put("cajas", fCajas)
            values.put("cantidad", fCantidad)
            values.put("piezas", fPiezas)
            values.put("precio", fPrecio)
            values.put("precioii", fPrecioII)
            values.put("dto", fDtoLin)
            values.put("dtoi", fDtoImp)
            values.put("dtoiii", fDtoImpII)
            values.put("codigoiva", fCodigoIva)
            values.put("tasa1", fTasa1)
            values.put("tasa2", fTasa2)
            values.put("lote", fLote)
            values.put("almacenPedido", fAlmacPedido)
            values.put("textolinea", fTextoLinea)

            // Si trabajamos con artículos habituales (p.ej. Pare Pere), grabaremos en el texto de la línea
            // el texto que tenga el artículo para el cliente del documento y el formato de la línea.
            if (fHayArtHabituales) values.put("textolinea", fTextoLinea)
            values.put("flag", fFlag)
            values.put("flag3", fFlag3)
            values.put("flag5", fFlag5)
            if (fInsertando) {
                values.put("linea", fLinea)
                values.put("articulo", fArticulo)
                values.put("codigo", fCodigo)
                values.put("descr", fDescr)
                values.put("formato", fFormatoLin)
                dbAlba.insert("tmphco", null, values)
            } else dbAlba.update("tmphco", values, "linea=$fLinea", null)
            refrescarHco()
        }
    }

    fun aceptarDatosDevolucion(fLinea: Int) {
        var fInsertando = true
        dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
            cTmpHco.moveToFirst()
            while (!cTmpHco.isAfterLast) {
                if (cTmpHco.getInt(cTmpHco.getColumnIndex("linea") ?: 0) == fLinea) fInsertando = false
                cTmpHco.moveToNext()
            }
            val values = ContentValues()
            values.put("cajas", fCajas)
            values.put("cantidad", fCantidad)
            values.put("piezas", fPiezas)
            values.put("precio", fPrecio)
            values.put("precioii", fPrecioII)
            values.put("dto", fDtoLin)
            values.put("dtoi", fDtoImp)
            values.put("dtoiii", fDtoImpII)
            values.put("codigoiva", fCodigoIva)
            values.put("tasa1", fTasa1)
            values.put("tasa2", fTasa2)
            values.put("incidencia", fIncidencia)
            if (fInsertando) {
                values.put("linea", fLinea)
                values.put("articulo", fArticulo)
                values.put("codigo", fCodigo)
                values.put("descr", fDescr)
                values.put("formato", fFormatoLin)
                dbAlba.insert("tmphco", null, values)
            } else dbAlba.update("tmphco", values, "linea=$fLinea", null)
        }
    }

    private fun refrescarHco() {
        cHco.close()
        abrir(fCliente)
    }

    fun borrar() {
        dbAlba.delete("tmphco", "1=1", null)
    }


    fun getTextoArtHabit(): String {
        return cHco.getString(cHco.getColumnIndex("texto") ?: 0) ?: ""
    }


    // Buscamos si el artículo está en el temporal y según el resultado, insertamos o editamos.
    // Esta función es casi idéntica a la de arriba (aceptarCambios), la diferencia está en que
    // localizamos la línea mediante el artículo.
    fun aceptarCambiosArt(queArticulo: Int) {
        var fInsertando = true
        if (queArticulo > 0) {
            // Compruebo si el artículo ya existe en la tabla temporal.
            dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
                cTmpHco.moveToFirst()
                while (!cTmpHco.isAfterLast) {
                    if (cTmpHco.getInt(cTmpHco.getColumnIndex("articulo") ?: 19) == queArticulo) {
                        fInsertando = false
                    }
                    cTmpHco.moveToNext()
                }
            }
        }
        val values = ContentValues()
        values.put("cajas", fCajas)
        values.put("cantidad", fCantidad)
        values.put("piezas", fPiezas)
        values.put("precio", fPrecio)
        values.put("precioii", fPrecioII)
        values.put("dto", fDtoLin)
        values.put("dtoi", fDtoImp)
        values.put("dtoiii", fDtoImpII)
        values.put("codigoiva", fCodigoIva)
        values.put("tasa1", fTasa1)
        values.put("tasa2", fTasa2)
        values.put("flag", fFlag)
        values.put("flag5", fFlag5)
        if (fInsertando) {
            values.put("articulo", fArticulo)
            values.put("codigo", fCodigo)
            values.put("descr", fDescr)
            values.put("formato", fFormatoLin)
            dbAlba.insert("tmphco", null, values)
        } else dbAlba.update("tmphco", values, "articulo=$queArticulo", null)
    }

}