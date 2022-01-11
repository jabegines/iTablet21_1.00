package es.albainformatica.albamobileandroid.impresion_informes

import android.annotation.SuppressLint
import android.content.Context
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import android.content.SharedPreferences
import es.albainformatica.albamobileandroid.database.MyDatabase
import android.preference.PreferenceManager
import datamaxoneil.printer.DocumentExPCL_LP
import datamaxoneil.connection.ConnectionBase
import datamaxoneil.connection.Connection_Bluetooth
import datamaxoneil.printer.ParametersExPCL_LP
import es.albainformatica.albamobileandroid.*
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * Created by jabegines on 12/07/2016.
 */
class ImprDocDatamaxApex2(contexto: Context): Runnable {
    private var fContexto: Context = contexto
    private var fDocumento: Documento = Comunicador.fDocumento
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private var fFormasPago: FormasPagoClase = FormasPagoClase(contexto)
    private var fPendiente: PendienteClase = PendienteClase(contexto)
    private lateinit var pref: SharedPreferences
    private var myBD: MyDatabase? = getInstance(contexto)

    private var fFtoCant: String = ""
    private var fFtoPrBase: String = ""
    private var fFtoImpBase: String = ""
    private var fFtoImpII: String = ""
    private var fVtaIvaIncluido: Boolean = false
    private var fAnchoPapel: Short = 0
    var fTerminado = false
    var fImprimiendo = false


    init {
        // Obtenemos el documento actual a través del comunicador.
        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoCant = fConfiguracion.formatoDecCantidad()
        fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoImpII = fConfiguracion.formatoDecImptesIva()
        fVtaIvaIncluido = fConfiguracion.ivaIncluido(fDocumento.fEmpresa.toString().toInt())

        // Leemos las preferencias de la aplicación;
        pref = PreferenceManager.getDefaultSharedPreferences(fContexto)
    }

    fun imprimir() {
        // Creamos y arrancamos el hilo. Una vez que arrancamos el hilo, se ejecutará el método run() de la actividad.
        val mThread = Thread(this)
        mThread.start()
    }

    override fun run() {
        fImprimiendo = true
        imprimirDoc()
    }

    private fun imprimirDoc() {
        val t: Thread = object : Thread() {
            override fun run() {
                fAnchoPapel = 48
                val docExPCLLP = DocumentExPCL_LP(5)
                imprCabecera(docExPCLLP)
                imprDatosClteYDoc(docExPCLLP)
                imprCabLineas(docExPCLLP)
                imprLineas(docExPCLLP)
                imprCabPie(docExPCLLP)
                imprBases(docExPCLLP)
                if (fDocumento.fTipoDoc == TIPODOC_FACTURA) imprFPago(docExPCLLP)
                imprPie(docExPCLLP)
                val printData: ByteArray = docExPCLLP.documentData
                var conn: ConnectionBase? = null
                try {
                    conn = Connection_Bluetooth.createClient(pref.getString("impresoraBT", ""))
                    if (!conn.getIsOpen()) {
                        conn.open()
                    }
                    conn.write(printData)
                    sleep(2000)
                    // Signals to close connection
                    conn.close()
                    fTerminado = true
                    fImprimiendo = false
                } catch (e: Exception) {
                    //signals to close connection
                    conn?.close()
                    e.printStackTrace()
                }
            }
        }
        t.start()
    }

    private fun imprCabecera(docExPCL_LP: DocumentExPCL_LP) {
        val docsCabPiesDao = myBD?.docsCabPiesDao()
        val cabeceraDoc = docsCabPiesDao?.cabeceraDoc(fDocumento.fEmpresa) ?: ""
        val lineasCabDoc = fConfiguracion.lineasCabDoc(fDocumento.fEmpresa)
        val lineasDobles = StringBuilder()
        for (x in 0 until fAnchoPapel) {
            lineasDobles.append("=")
        }
        docExPCL_LP.writeText(lineasDobles.toString())
        docExPCL_LP.writeText(cabeceraDoc)
        docExPCL_LP.writeText(lineasCabDoc)
        docExPCL_LP.writeText(lineasDobles.toString())
    }

    private fun imprDatosClteYDoc(docExPCL_LP: DocumentExPCL_LP) {
        var cCadena: String = ajustarCadena(ponerCeros(fDocumento.fClientes.fCodigo.toString(), ancho_codclte) + " " +
                fDocumento.fClientes.fNombre, 35, true)
        docExPCL_LP.writeText(cCadena)
        cCadena = ajustarCadena(fDocumento.fClientes.fNomComercial, 35, true)
        docExPCL_LP.writeText(cCadena)
        cCadena = ajustarCadena("C.I.F.: " + fDocumento.fClientes.getCIF(), 35, true)
        docExPCL_LP.writeText(cCadena)
        cCadena = ajustarCadena(fDocumento.fClientes.getDireccion(), 35, true)
        docExPCL_LP.writeText(cCadena)
        cCadena = ajustarCadena(
            fDocumento.fClientes.getCodPostal() + " " + fDocumento.fClientes.getPoblacion(),
            35,
            true
        )
        docExPCL_LP.writeText(cCadena)
        cCadena = ajustarCadena(fDocumento.fClientes.getProvincia(), 35, true)
        docExPCL_LP.writeText(cCadena)
        docExPCL_LP.writeText("")
        cCadena = "Doc: " + tipoDocAsString(fDocumento.fTipoDoc)
        cCadena = ajustarCadena(cCadena, 15, true)
        docExPCL_LP.writeTextPartial(cCadena)
        cCadena = "   Num.: " + fDocumento.serie + "/" + fDocumento.numero
        docExPCL_LP.writeText(cCadena)
        cCadena = "Hora: " + fDocumento.fHora
        cCadena = ajustarCadena(cCadena, 15, true)
        docExPCL_LP.writeTextPartial(cCadena)
        cCadena = "   Fecha: " + fDocumento.fFecha
        docExPCL_LP.writeText(cCadena)
        docExPCL_LP.writeText("")
    }

    private fun ajustarCadena(cCadena: String, maxLong: Int, fPorLaDerecha: Boolean): String {
        var result = cCadena
        // Si la cadena supera el máximo de caracteres, la recortamos. En cambio, si
        // no llega a esta cifra, le añadimos espacios al final.
        if (result.length > maxLong) result =
            result.substring(0, maxLong) else if (result.length < maxLong) {
            result = if (fPorLaDerecha) result + StringOfChar(
                " ",
                maxLong - result.length
            ) else StringOfChar(" ", maxLong - result.length) + result
        }
        return result
    }

    private fun imprCabLineas(docExPCL_LP: DocumentExPCL_LP) {
        val lineaSimple = StringBuilder()
        val cCadena: String = ("ARTICULO"
                + StringOfChar(" ", 13) + "CAJAS"
                + StringOfChar(" ", 3) + "UNID"
                + StringOfChar(" ", 2) + "PRECIO"
                + StringOfChar(" ", 2) + "TOTAL")
        docExPCL_LP.writeText(cCadena)
        for (x in 0 until fAnchoPapel) {
            lineaSimple.append("-")
        }
        docExPCL_LP.writeText(lineaSimple.toString())
    }

    @SuppressLint("Range")
    private fun imprLineas(docExPCL_LP: DocumentExPCL_LP) {
        var cCadena: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        var sPrecio: String
        var sImpte: String
        val lDescr = 19
        val lCajas = 6
        val lCant = 6
        val lPrecio = 6
        val lImpte = 7

        fDocumento.cLineas.moveToFirst()
        while (!fDocumento.cLineas.isAfterLast) {
            sDescr = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("descr"))
            sCajas = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cajas"))
            sCant = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cantidad"))
            if (fVtaIvaIncluido) {
                sPrecio =
                    fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("precioii"))
                sImpte =
                    fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("importeii"))
            } else {
                sPrecio =
                    fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("precio"))
                sImpte =
                    fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("importe"))
            }
            cCadena = ajustarCadena(sDescr, lDescr, true)
            docExPCL_LP.writeTextPartial(cCadena)
            val dCajas = sCajas.toDouble()
            val dCant = sCant.toDouble()
            sCajas = String.format(fFtoCant, dCajas)
            sCant = String.format(fFtoCant, dCant)
            val dPrecio = sPrecio.toDouble()
            val dImpte = sImpte.toDouble()
            sPrecio = String.format(fFtoPrBase, dPrecio)
            sImpte = String.format(fFtoImpBase, dImpte)
            cCadena = (" " + ajustarCadena(sCajas, lCajas, false) + " "
                    + ajustarCadena(sCant, lCant, false) + " "
                    + ajustarCadena(sPrecio, lPrecio, false) + " "
                    + ajustarCadena(sImpte, lImpte, false))
            docExPCL_LP.writeText(cCadena)
            fDocumento.cLineas.moveToNext()
        }
    }

    private fun imprCabPie(docExPCL_LP: DocumentExPCL_LP) {
        val lineaSimple = StringBuilder()
        for (x in 0 until fAnchoPapel) {
            lineaSimple.append("-")
        }
        docExPCL_LP.writeText(lineaSimple.toString())
        val cCadena: String = ("  VENTA"
                + StringOfChar(" ", 8) + "DTO" + StringOfChar(" ", 7)
                + "NETO" + StringOfChar(" ", 4) + "%IVA"
                + StringOfChar(" ", 7) + "IVA")
        // + Miscelan.StringOfChar(" ", 3) + "%REC" + Miscelan.StringOfChar(" ", 4) + "REC";
        docExPCL_LP.writeText(cCadena)
        docExPCL_LP.writeText(lineaSimple.toString())
    }

    private fun imprBases(docExPCL_LP: DocumentExPCL_LP) {
        var cCadena: String
        var sBruto: String
        var sImpDto: String
        var sBase: String
        var sPorcIva: String
        var sImpIva: String
        var hayRecargo = false
        val lBruto = 9
        val lImpDto = 8
        val lBase = 9
        val lImpIva = 9
        val lTotal = 9
        for (x in fDocumento.fBases.fLista) {
            if (x.fBaseImponible != 0.0) {
                val dImpDto = x.fImpDtosPie
                if (fVtaIvaIncluido) {
                    // Si vendemos iva incluído, x.ImpteBruto es la suma de los importes iva incluído, mientras que lo que
                    // queremos imprimir es el importe bruto antes de descuentos pero base imponible.
                    sBruto = String.format(fFtoImpII, x.fBaseImponible + x.fImpDtosPie)
                    sImpDto = String.format(fFtoImpII, dImpDto, false)
                } else {
                    sBruto = String.format(fFtoImpBase, x.fImpteBruto)
                    sImpDto = String.format(fFtoImpBase, dImpDto, false)
                }
                sBase = String.format(fFtoImpBase, x.fBaseImponible)
                sPorcIva = String.format(Locale.getDefault(), "%.2f", x.fPorcIva)
                sImpIva = String.format(fFtoImpBase, x.fImporteIva)
                cCadena = (ajustarCadena(sBruto, lBruto, false)
                        + StringOfChar(" ", 1)
                        + ajustarCadena(sImpDto, lImpDto, false)
                        + StringOfChar(" ", 3)
                        + ajustarCadena(sBase, lBase, false)
                        + StringOfChar(" ", 3) + ajustarCadena(sPorcIva, 5, false)
                        + StringOfChar(" ", 1)
                        + ajustarCadena(sImpIva, lImpIva, false))
                hayRecargo = hayRecargo || x.fImporteRe != 0.0
                docExPCL_LP.writeText(cCadena)
            }
        }
        val sTotal: String = String.format(fFtoImpII, fDocumento.fBases.totalConImptos)

        // Si el documento tiene recargo de equivalencia lo imprimimos ahora.
        if (hayRecargo) imprRecargo(docExPCL_LP)
        docExPCL_LP.writeText("")
        docExPCL_LP.writeText("")
        val paramExPCLLP = ParametersExPCL_LP()
        paramExPCLLP.fontIndex = 10
        docExPCL_LP.writeText("TOTAL IMPORTE:", paramExPCLLP)
        docExPCL_LP.writeText(ajustarCadena(sTotal, lTotal, false) + "  Euros", paramExPCLLP)
        docExPCL_LP.writeText("")
    }

    private fun imprRecargo(docExPCL_LP: DocumentExPCL_LP) {
        var cCadena: String
        var sPorcRe: String
        var sImpRe: String
        val lineaSimple = StringBuilder()
        val lImpRe = 8
        for (x in 0..14) {
            lineaSimple.append("-")
        }
        docExPCL_LP.writeText(lineaSimple.toString())
        docExPCL_LP.writeText("%REC" + StringOfChar(" ", 6) + "REC")
        docExPCL_LP.writeText(lineaSimple.toString())
        for (x in fDocumento.fBases.fLista) {
            if (x.fBaseImponible != 0.0) {
                if (x.fImporteRe != 0.0) {
                    sPorcRe = String.format(fFtoImpBase, x.fPorcRe)
                    sImpRe = String.format(fFtoImpBase, x.fImporteRe)
                    cCadena =
                        ajustarCadena(sPorcRe, 5, false) + StringOfChar(" ", 1) + ajustarCadena(
                            sImpRe,
                            lImpRe,
                            false
                        )
                    docExPCL_LP.writeText(cCadena)
                }
            }
        }
    }

    private fun imprFPago(docExPCL_LP: DocumentExPCL_LP) {
        var cCadena: String
        val sImpte: String
        val sCobrado: String
        val sPdte: String
        val lineaSimple = StringBuilder()
        val lImptes = 9
        if (fDocumento.fSerieExenta) {
            cCadena = "Inversión del sujeto pasivo"
            docExPCL_LP.writeText(cCadena)
        }
        cCadena = "Forma de pago: " + fFormasPago.getDescrFPago(fDocumento.fPago)
        docExPCL_LP.writeText(cCadena)
        if (fPendiente.abrirDocumento()) {
            for (x in 0..34) {
                lineaSimple.append("-")
            }
            docExPCL_LP.writeText(lineaSimple.toString())
            cCadena = (StringOfChar(" ", 2) + "IMPORTE"
                    + StringOfChar(" ", 2) + "ENTREGADO"
                    + StringOfChar(" ", 2) + "PENDIENTE")
            docExPCL_LP.writeText(cCadena)
            docExPCL_LP.writeText(lineaSimple.toString())
            val dImporte = fPendiente.importe.toDouble()
            sImpte = String.format(fFtoImpII, dImporte)
            val dCobrado = fPendiente.cobrado.toDouble()
            sCobrado = String.format(fFtoImpII, dCobrado)
            val dPdte = dImporte - dCobrado
            sPdte = String.format(fFtoImpII, dPdte)
            cCadena = (ajustarCadena(sImpte, lImptes, false)
                    + StringOfChar(" ", 2)
                    + ajustarCadena(sCobrado, lImptes, false)
                    + StringOfChar(" ", 2)
                    + ajustarCadena(sPdte, lImptes, false))
            docExPCL_LP.writeText(cCadena)
        }
    }

    private fun imprPie(docExPCL_LP: DocumentExPCL_LP) {
        val lineasPieDoc = fConfiguracion.lineasPieDoc(fDocumento.fEmpresa)
        docExPCL_LP.writeText(lineasPieDoc)

        // En albaranes imprimimos las observaciones del documento.
        if (fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            docExPCL_LP.writeText(fDocumento.fObs1)
            docExPCL_LP.writeText(fDocumento.fObs2)
        }
        docExPCL_LP.writeText("")
        docExPCL_LP.writeText("")
        docExPCL_LP.writeText("")
        docExPCL_LP.writeText("")
    }
}