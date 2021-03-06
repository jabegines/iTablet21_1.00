package es.albainformatica.albamobileandroid.impresion_informes

import android.annotation.SuppressLint
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import android.content.SharedPreferences
import es.albainformatica.albamobileandroid.database.MyDatabase
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import androidx.preference.PreferenceManager
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CargasDao
import es.albainformatica.albamobileandroid.dao.CargasLineasDao
import es.albainformatica.albamobileandroid.entity.CargasEnt
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class ImprIntermecPB51(contexto: Context): Runnable {
    private var fContexto: Context = contexto
    private var fDocumento: Documento = Comunicador.fDocumento
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private var fFormasPago: FormasPagoClase = FormasPagoClase(contexto)
    private var fPendiente: PendienteClase = PendienteClase(contexto)
    private lateinit var prefs: SharedPreferences

    private var myBD: MyDatabase? = getInstance(contexto)

    private var fFtoCant: String = ""
    private var fFtoPrBase: String = ""
    private var fFtoImpBase: String = ""
    private var fFtoImpII: String = ""
    private var fVtaIvaIncluido: Boolean = false
    private var fNumLineasDoc = 0
    private var fPrimeraLinea = 0
    private var fPosicionCorte = 0
    private var fPosicionPie = 0
    private var fLineasImpr = 0

    private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mBluetoothDevice: BluetoothDevice

    var fTerminado = false
    var fImprimiendo = false
    private var queImprimir: Short = 1
    private var fCargaId = 0
    private val fMargenIzq = stringOfChar(" ", 5)

    private val ccSaltoLinea = "\n"
    private val ccDobleAncho = 14.toChar().toString()
    private val ccNormal = 20.toChar().toString()
    private val fImprimirDocumento: Short = 1
    private val fImprimirCarga: Short = 2



    init {
        inicializarControles()
    }

    private fun destruir() {
        try {
            mBluetoothSocket.close()
        } catch (e: Exception) {
            //
        }
    }

    fun imprimir() {
        // Intentamos conectar con Bluetooth. Para ello pasamos la direcci??n de la impresora.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(fContexto, "No se pudo conectar con Bluetooth", Toast.LENGTH_SHORT)
                .show()
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                fContexto.startActivity(enableBtIntent)
            } else {
                // Leemos la direcci??n de la impresora Bluetooth de las preferencias.
                val mDeviceAddress = prefs.getString("impresoraBT", "")
                queImprimir = fImprimirDocumento
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress)
                mBluetoothConnectProgressDialog = ProgressDialog.show(
                    fContexto,
                    "Conectando...",
                    mBluetoothDevice.name,
                    true,
                    false
                )
                val mBluetoothConnectThread = Thread(this)
                mBluetoothConnectThread.start()
                // Una vez conectados, arrancamos el hilo. Una vez que arrancamos el
                // hilo, se ejecutar?? el m??todo run() de la actividad.
            }
        }
    }

    fun imprimirCarga(queCarga: Int) {
        // Intentamos conectar con Bluetooth. Para ello pasamos la direcci??n de la impresora.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(fContexto, "No se pudo conectar con Bluetooth", Toast.LENGTH_SHORT)
                .show()
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                fContexto.startActivity(enableBtIntent)
            } else {
                // Leemos la direcci??n de la impresora Bluetooth de las preferencias.
                val mDeviceAddress = prefs.getString("impresoraBT", "")
                fCargaId = queCarga
                queImprimir = fImprimirCarga
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress)
                mBluetoothConnectProgressDialog = ProgressDialog.show(
                    fContexto,
                    "Conectando...",
                    mBluetoothDevice.name,
                    true,
                    false
                )
                val mBluetoothConnectThread = Thread(this)
                mBluetoothConnectThread.start()
                // Una vez conectados, arrancamos el hilo. Una vez que arrancamos el
                // hilo, se ejecutar?? el m??todo run() de la actividad.
            }
        }
    }

    private fun inicializarControles() {
        fFtoCant = fConfiguracion.formatoDecCantidad()
        fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoImpII = fConfiguracion.formatoDecImptesIva()
        fVtaIvaIncluido = fConfiguracion.ivaIncluido(fDocumento.fEmpresa)
        fLineasImpr = 0

        // Leemos las preferencias de la aplicaci??n;
        prefs = PreferenceManager.getDefaultSharedPreferences(fContexto)
        fNumLineasDoc = prefs.getString("lineas_doc", "48")?.toInt() ?: 48
        fPrimeraLinea = prefs.getString("primera_linea", "8")?.toInt() ?: 8
        fPosicionCorte = prefs.getString("posicion_corte", "8")?.toInt() ?: 8
        fPosicionPie = prefs.getString("posicion_pie", "37")?.toInt() ?: 37
    }

    override fun run() {
        try {
            // Obtenemos un bluetoothsocket y lo conectamos. A partir de entonces,
            // llamamos a imprimirDoc().
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
            mBluetoothAdapter.cancelDiscovery()
            mBluetoothSocket.connect()
            mHandler.sendEmptyMessage(0)
            fImprimiendo = true
            if (queImprimir == fImprimirDocumento) imprimirDoc() else if (queImprimir == fImprimirCarga) imprimeCarga()
        } catch (eConnectException: IOException) {
            closeSocket(mBluetoothSocket)
        }
    }

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            mBluetoothConnectProgressDialog.dismiss()
        }
    }

    private fun closeSocket(nOpenSocket: BluetoothSocket) {
        try {
            nOpenSocket.close()
        } catch (ex: IOException) {
            //
        }
    }

    private fun imprimeCarga() {
        val t: Thread = object : Thread() {
            override fun run() {
                val texto: String
                try {
                    val os = mBluetoothSocket.outputStream
                    texto = imprCabeceraCarga()
                    os.write(texto.toByteArray())
                    // Pausa
                    SystemClock.sleep(500)
                    imprLineasCarga(os)
                    SystemClock.sleep(500)

                    // Llamo a destruir porque he comprobado que la clase no pasa por el m??todo onDestroy() (supongo que porque
                    // no hereda de Activity), as?? me aseguro de cerrar el socket y los dem??s objetos abiertos.
                    destruir()
                    fTerminado = true
                    fImprimiendo = false
                } catch (e: Exception) {
                    fTerminado = true
                }
            }
        }
        t.start()
    }

    private fun imprimirDoc() {
        val t: Thread = object : Thread() {
            override fun run() {
                var texto: String
                try {
                    val os = mBluetoothSocket.outputStream
                    texto = imprCabecera()
                    texto += imprDatosClteYDoc()
                    os.write(texto.toByteArray())
                    texto = ""
                    // Pausa.
                    SystemClock.sleep(500)
                    texto += imprCabLineas()
                    fLineasImpr = 13 + fPosicionCorte
                    os.write(texto.toByteArray())
                    imprLineasPositivas(os)
                    imprLineasNegativas(os)
                    imprCabPie(os)
                    imprBases(os)
                    texto = ""
                    // Nueva pausa.
                    SystemClock.sleep(500)
                    if (fDocumento.fTipoDoc == TIPODOC_FACTURA) {
                        texto += imprFPago()
                        fLineasImpr += 5
                    }
                    texto += imprPie()
                    os.write(texto.toByteArray())

                    // Llamo a destruir porque he comprobado que la clase no pasa por el m??todo onDestroy() (supongo que porque
                    // no hereda de Activity), as?? me aseguro de cerrar el socket y los dem??s objetos abiertos.
                    destruir()
                    fTerminado = true
                    fImprimiendo = false
                } catch (e: Exception) {
                    fTerminado = true
                }
            }
        }
        t.start()
    }

    private fun imprFPago(): String {
        val sImpte: String
        val sCobrado: String
        val sPdte: String
        var lineaSimple = StringBuilder(fMargenIzq)
        val lImptes = 9
        var result = ccSaltoLinea
        if (fDocumento.fSerieExenta) result =
            result + "Inversi??n del sujeto pasivo" + ccSaltoLinea
        result =
            result + fMargenIzq + "Forma de pago: " + fFormasPago.getDescrFPago(fDocumento.fPago)
        result += ccSaltoLinea
        if (fPendiente.abrirDocumento()) {
            for (x in 0..39) {
                lineaSimple.append("-")
            }
            result = result + lineaSimple + ccSaltoLinea
            result = (result + fMargenIzq + stringOfChar(" ", 2) + "IMPORTE"
                    + stringOfChar(" ", 7) + "ENTREGADO"
                    + stringOfChar(" ", 6) + "PENDIENTE" + ccSaltoLinea)
            lineaSimple = StringBuilder(fMargenIzq)
            for (x in 0..39) {
                lineaSimple.append("-")
            }
            result = result + lineaSimple + ccSaltoLinea
            val dImporte = fPendiente.importe.toDouble()
            sImpte = String.format(fFtoImpII, dImporte)
            val dCobrado = fPendiente.cobrado.toDouble()
            sCobrado = String.format(fFtoImpII, dCobrado)
            val dPdte = dImporte - dCobrado
            sPdte = String.format(fFtoImpII, dPdte)
            result = (result + fMargenIzq + ajustarCadena(sImpte, lImptes, false)
                    + stringOfChar(" ", 7)
                    + ajustarCadena(sCobrado, lImptes, false)
                    + stringOfChar(" ", 6)
                    + ajustarCadena(sPdte, lImptes, false))
            result += ccSaltoLinea
        }
        return result
    }

    private fun imprBases(os: OutputStream) {
        val texto = StringBuilder()
        var sBruto: String
        var sImpDto: String
        var sBase: String
        var sPorcIva: String
        var sImpIva: String
        var sPorcRe: String
        var sImpRe: String
        val sTotal: String
        val lBruto = 10
        val lImpDto = 10
        val lBase = 10
        val lImpIva = 8
        val lImpRe = 8
        val lTotal = 8

        try {
            for (x in fDocumento.fBases.fLista) {
                if (x.fBaseImponible != 0.0) {
                    val dImpDto = x.fImpDtosPie
                    if (fVtaIvaIncluido) {
                        // Si vendemos iva inclu??do, x.ImpteBruto es la suma de los importes iva inclu??do, mientras que lo que
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
                    sPorcRe = String.format(fFtoImpBase, x.fPorcRe)
                    sImpRe = String.format(fFtoImpBase, x.fImporteRe)
                    texto.append(fMargenIzq).append(ajustarCadena(sBruto, lBruto, false))
                        .append(stringOfChar(" ", 1))
                        .append(ajustarCadena(sImpDto, lImpDto, false)).append(stringOfChar(" ", 1))
                        .append(ajustarCadena(sBase, lBase, false)).append(stringOfChar(" ", 2))
                        .append(ajustarCadena(sPorcIva, 7, false)).append(stringOfChar(" ", 1))
                        .append(ajustarCadena(sImpIva, lImpIva, false)).append(stringOfChar(" ", 1))
                    if (x.fImporteRe != 0.0) texto.append(ajustarCadena(sPorcRe, 8, false))
                        .append(stringOfChar(" ", 3)).append(
                        ajustarCadena(
                            sImpRe,
                            lImpRe,
                            false
                        )
                    ) else texto.append(stringOfChar(" ", 6 + lImpRe))
                    texto.append(ccSaltoLinea)
                    fLineasImpr++
                }
            }
            sTotal = String.format(fFtoImpII, fDocumento.fBases.totalConImptos)
            texto.append(ccSaltoLinea).append(ccSaltoLinea)
            texto.append(stringOfChar(" ", 27)).append("TOTAL IMPORTE: ").append(ccDobleAncho)
                .append(ajustarCadena(sTotal, lTotal, false)).append(
                    ccNormal
            ).append("  Euros").append(ccSaltoLinea)
            fLineasImpr += 2
            os.write(texto.toString().toByteArray())
            // Pausa
            SystemClock.sleep(500)
        } catch (e: Exception) {
            //
        }
    }

    @SuppressLint("Range")
    private fun imprLineasNegativas(os: OutputStream) {
        val result = StringBuilder()
        var sCodigo: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        var sPrecio: String
        var sImpte: String
        var sLote: String
        var fIncidencia: Int
        val lCodigo = 7
        val lDescr = 30
        val lCajas = 6
        val lCant = 7
        val lPrecio = 7
        val lImpte = 8
        val lLote = 20
        var sumaCajas = 0.0
        var sumaCant = 0.0

        val tiposIncDao = myBD?.tiposIncDao()

        for (linea in fDocumento.lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sCajas = linea.cajas
            sCant = linea.cantidad
            val dCant = sCant.toDouble()
            if (dCant < 0.0) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII
                    sImpte = linea.importeII
                } else {
                    sPrecio = linea.precio
                    sImpte = linea.importe
                }
                result.append(fMargenIzq).append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                    .append(ajustarCadena(sDescr, lDescr, true))
                val dCajas = sCajas.toDouble()
                sumaCant += dCant
                sumaCajas += dCajas
                sCajas = String.format(fFtoCant, dCajas)
                sCant = String.format(fFtoCant, dCant)
                val dPrecio = sPrecio.toDouble()
                val dImpte = sImpte.toDouble()
                sPrecio = String.format(fFtoPrBase, dPrecio)
                sImpte = String.format(fFtoImpBase, dImpte)
                result.append(" ").append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                    .append(ajustarCadena(sCant, lCant, false)).append(" ")
                    .append(ajustarCadena(sPrecio, lPrecio, false)).append(" ")
                    .append(ajustarCadena(sImpte, lImpte, false))
                result.append(ccSaltoLinea)
                fLineasImpr++

                // Si la l??nea tiene n??mero de lote lo imprimimos.
                if (linea.lote != "") {
                    sLote = linea.lote
                    result.append(fMargenIzq).append("Numero lote: ").append(ajustarCadena(sLote, lLote, true))
                    result.append(ccSaltoLinea)
                    fLineasImpr++
                }
                // Si la l??nea tiene incidencia la imprimimos
                if (linea.tipoIncId > 0) {
                    fIncidencia = linea.tipoIncId
                    val queDescrInc = tiposIncDao?.dimeDescripcion(fIncidencia) ?: ""
                    if (queDescrInc != "") {
                        result.append(fMargenIzq).append("Incidencia: ").append(fIncidencia).append(" ").append(queDescrInc)
                        result.append(ccSaltoLinea)
                    }
                }
            }
        }
        result.append(ccSaltoLinea)
        if (sumaCajas != 0.0 || sumaCant != 0.0) {
            sCajas = String.format(fFtoCant, sumaCajas)
            sCant = String.format(fFtoCant, sumaCant)
            result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 32))
                .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                .append(ajustarCadena(sCant, lCant, false)).append(
                    ccSaltoLinea
            )
        }
        try {
            os.write(result.toString().toByteArray())
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (e: Exception) {
            //
        }
    }

    @SuppressLint("Range")
    private fun imprLineasPositivas(os: OutputStream) {
        val result = StringBuilder()
        var sCodigo: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        var sPrecio: String
        var sDto: String
        var sImpte: String
        var sLote: String
        var fIncidencia: Int
        val lCodigo = 7
        val lDescr = 30
        val lCajas = 6
        val lCant = 7
        val lPrecio = 7
        val lImpte = 8
        val lLote = 20
        var sumaCajas = 0.0
        var sumaCant = 0.0

        val tiposIncDao = myBD?.tiposIncDao()

        for (linea in fDocumento.lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sCajas = linea.cajas
            sCant = linea.cantidad
            val dCant = sCant.toDouble()
            if (dCant >= 0.0) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII
                    sImpte = linea.importeII
                } else {
                    sPrecio = linea.precio
                    sImpte = linea.importe
                }
                result.append(fMargenIzq).append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                    .append(ajustarCadena(sDescr, lDescr, true))
                val dCajas = sCajas.toDouble()
                sumaCant += dCant
                sumaCajas += dCajas
                sCajas = String.format(fFtoCant, dCajas)
                sCant = String.format(fFtoCant, dCant)
                val dPrecio = sPrecio.toDouble()
                val dImpte = sImpte.toDouble()

                // Si la l??nea es sin cargo lo indicamos
                if (linea.flag and FLAGLINEAVENTA_SIN_CARGO > 0) {
                    sPrecio = "SIN"
                    sImpte = "CARGO"
                } else {
                    sPrecio = String.format(fFtoPrBase, dPrecio)
                    sImpte = String.format(fFtoImpBase, dImpte)
                }
                result.append(" ").append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                    .append(ajustarCadena(sCant, lCant, false)).append(" ")
                    .append(ajustarCadena(sPrecio, lPrecio, false)).append(" ")
                    .append(ajustarCadena(sImpte, lImpte, false))
                result.append(ccSaltoLinea)
                fLineasImpr++

                // Si la l??nea tiene n??mero de lote lo imprimimos.
                if (linea.lote != "") {
                    sLote = linea.lote
                    result.append(fMargenIzq).append("Numero lote: ")
                        .append(ajustarCadena(sLote, lLote, true))
                    result.append(ccSaltoLinea)
                    fLineasImpr++
                }
                // Si la l??nea tiene incidencia la imprimimos
                if (linea.tipoIncId > 0) {
                    fIncidencia = linea.tipoIncId
                    val queDescrInc = tiposIncDao?.dimeDescripcion(fIncidencia) ?: ""
                    if (queDescrInc != "") {
                        result.append(fMargenIzq).append("Incidencia: ").append(fIncidencia).append(" ").append(queDescrInc)
                        result.append(ccSaltoLinea)
                    }
                    fLineasImpr++
                }
                // Si la l??nea tiene descuento lo imprimimos
                if (linea.dto != "") {
                    sDto = linea.dto
                    result.append(fMargenIzq).append("% dto.: ")
                        .append(ajustarCadena(sDto, 5, false))
                    result.append(ccSaltoLinea)
                    fLineasImpr++
                }
            }
        }
        result.append(ccSaltoLinea)
        sCajas = String.format(fFtoCant, sumaCajas)
        sCant = String.format(fFtoCant, sumaCant)
        result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 32))
            .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
            .append(ajustarCadena(sCant, lCant, false)).append(
                ccSaltoLinea
        )
        val lineaSimple = StringBuilder(fMargenIzq)
        for (x in 0..69) {
            lineaSimple.append("-")
        }
        result.append(lineaSimple).append(ccSaltoLinea).append(ccSaltoLinea)
        try {
            os.write(result.toString().toByteArray())
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (e: Exception) {
            //
        }
    }

    private fun imprCabPie(os: OutputStream) {
        var texto = ""
        val lineaSimple = StringBuilder(fMargenIzq)
        try {
            //for (int x = fLineasImpr; x < fPosicionPie; x++) {
            texto += stringOfChar(ccSaltoLinea, 2)
            //}
            os.write(texto.toByteArray())
            for (x in 0..69) {
                lineaSimple.append("-")
            }
            texto = lineaSimple.toString() + ccSaltoLinea
            texto = (texto + fMargenIzq + stringOfChar(" ", 5) + "VENTA" + stringOfChar(
                " ",
                8
            ) + "DTO" + stringOfChar(" ", 7)
                    + "NETO" + stringOfChar(" ", 5) + "%IVA" + stringOfChar(
                " ",
                6
            ) + "IVA" + stringOfChar(" ", 5)
                    + "%REC" + stringOfChar(" ", 7) + "REC" + ccSaltoLinea)
            texto = texto + lineaSimple + ccSaltoLinea
            os.write(texto.toByteArray())
        } catch (e: Exception) {
            //
        }
        // Pausa
        SystemClock.sleep(500)
        fLineasImpr = fPosicionPie + 3
    }

    private fun imprCabLineas(): String {
        val lineaSimple = StringBuilder(fMargenIzq)
        var result: String = (fMargenIzq + "COD." + stringOfChar(" ", 4) + "ARTICULO" + stringOfChar(
            " ",
            24
        ) + "CAJAS"
                + stringOfChar(" ", 4) + "UNID" + stringOfChar(" ", 2) + "PRECIO"
                + stringOfChar(" ", 4) + "TOTAL" + ccSaltoLinea)
        for (x in 0..69) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        return result
    }

    private fun imprDatosClteYDoc(): String {
        var result: String
        var cCadena: String
        val sLongDatosClte: Short = 50
        cCadena = ajustarCadena(fMargenIzq + ponerCeros(fDocumento.fClientes.fCodigo, ancho_codclte) + " " +
                fDocumento.fClientes.fNombre, sLongDatosClte.toInt(), true)
        result = cCadena
        cCadena = stringOfChar(" ", 5) + "Vendedor: " + fConfiguracion.vendedor()
        result = result + cCadena + ccSaltoLinea
        cCadena = ajustarCadena(fMargenIzq + fDocumento.fClientes.fNomComercial, sLongDatosClte.toInt(), true)
        result = result + cCadena + ccSaltoLinea
        result += ajustarCadena(fMargenIzq + fDocumento.fClientes.fDireccion, sLongDatosClte.toInt(), true)
        cCadena = stringOfChar(" ", 5) + "Hora: " + fDocumento.fHora
        result += cCadena
        result += ccSaltoLinea
        result += ajustarCadena(fMargenIzq + fDocumento.fClientes.fCodPostal + " " + fDocumento.fClientes.fPoblacion, sLongDatosClte.toInt(), true)
        cCadena = stringOfChar(" ", 5) + "Fecha: " + fDocumento.fFecha
        result += cCadena
        result += ccSaltoLinea
        result += ajustarCadena(fMargenIzq + fDocumento.fClientes.fProvincia, sLongDatosClte.toInt(), true)
        result = result + stringOfChar(" ", 5) + "Doc: " + tipoDocAsString(fDocumento.fTipoDoc)
        result += ccSaltoLinea
        result += ajustarCadena(fMargenIzq + "C.I.F.: " + fDocumento.fClientes.fCif, sLongDatosClte.toInt(), true)
        result = result + stringOfChar(" ", 5) + "Num.: " + fDocumento.serie + "/" + fDocumento.numero
        result = result + ccSaltoLinea + ccSaltoLinea + ccSaltoLinea
        return result
    }

    private fun ajustarCadena(cCadena: String, maxLong: Int, fPorLaDerecha: Boolean): String {
        var result = cCadena
        // Si la cadena supera el m??ximo de caracteres, la recortamos. En cambio, si no llega a esta cifra, le a??adimos espacios al final.
        if (result.length > maxLong) result =
            result.substring(0, maxLong) else if (result.length < maxLong) {
            result = if (fPorLaDerecha) result + stringOfChar(
                " ",
                maxLong - result.length
            ) else stringOfChar(" ", maxLong - result.length) + result
        }
        return result
    }

    private fun imprPie(): String {
        val result: StringBuilder
        val lineasPieDoc = fConfiguracion.lineasPieDoc(fDocumento.fEmpresa)
        result = StringBuilder(ccSaltoLinea + lineasPieDoc + ccSaltoLinea)
        fLineasImpr += 2

        // En albaranes imprimimos las observaciones del documento.
        if (fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            result.append(fDocumento.fObs1)
            result.append(ccSaltoLinea)
            result.append(fDocumento.fObs2)
            fLineasImpr += 1
        }

        // Avanzamos el papel hasta el punto de corte
        for (x in fLineasImpr until fNumLineasDoc + fPosicionCorte - 1) {
            result.append(ccSaltoLinea)
        }
        return result.toString()
    }

    private fun imprCabecera(): String {
        val docsCabPiesDao = myBD?.docsCabPiesDao()
        val cabeceraDoc = docsCabPiesDao?.cabeceraDoc(fDocumento.fEmpresa) ?: ""
        val lineasCabDoc = fConfiguracion.lineasCabDocConMargIzq(fDocumento.fEmpresa, fMargenIzq)
        val result = StringBuilder(fMargenIzq)
        val lineasDobles = StringBuilder()
        fLineasImpr = fPosicionCorte

        // Avanzamos hasta la primera l??nea de impresi??n (nos sirve para documentos con la cabecera preimpresa)
        for (x in fLineasImpr until fPrimeraLinea) {
            result.append(ccSaltoLinea)
        }
        for (x in 0..69) {
            lineasDobles.append("=")
        }
        result.append(lineasDobles).append(ccSaltoLinea)
        result.append(fMargenIzq).append(ccDobleAncho)
        result.append(cabeceraDoc).append(ccSaltoLinea)
        result.append(ccNormal)
        result.append(lineasCabDoc).append(ccSaltoLinea)
        result.append(fMargenIzq).append(lineasDobles).append(ccSaltoLinea)
        return result.toString()
    }

    @SuppressLint("Range")
    private fun imprCabeceraCarga(): String {
        var result = ""
        val lineasDobles = StringBuilder()
        val lineaSimple = StringBuilder()

        val cargasDao: CargasDao? = getInstance(fContexto)?.cargasDao()
        val cargaEnt = cargasDao?.getCarga(fCargaId) ?: CargasEnt()

        if (cargaEnt.cargaId > 0) {
            for (x in 0..62) {
                lineasDobles.append("=")
            }
            result += stringOfChar(ccSaltoLinea, 3)
            result = result + fMargenIzq + ccDobleAncho + "Nueva carga  "
            result += cargaEnt.fecha
            result = result + stringOfChar(" ", 3) + cargaEnt.hora
            result += ccSaltoLinea
            result = result + fMargenIzq + lineasDobles + stringOfChar(ccSaltoLinea, 4)
            result += ccNormal
            result = result + fMargenIzq + "Codigo" + stringOfChar(" ", 2) + "Descripcion" + stringOfChar(" ", 32) +
                    "Cajas" + stringOfChar(" ", 2) + "Cantidad" + ccSaltoLinea
            for (x in 0..65) {
                lineaSimple.append("-")
            }
            result = result + fMargenIzq + lineaSimple + ccSaltoLinea
        }

        return result
    }

    @SuppressLint("Range")
    private fun imprLineasCarga(os: OutputStream) {
        val result = StringBuilder()
        var sCodigo: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        val lCodigo = 7
        val lDescr = 40
        val lCajas = 7
        val lCant = 9
        var sumaCajas = 0.0
        var sumaCant = 0.0

        val lineasCargas: CargasLineasDao? = getInstance(fContexto)?.cargasLineasDao()
        val lLineas = lineasCargas?.getCarga(fCargaId) ?: emptyList<DatosDetCarga>().toMutableList()

        if (lLineas.isNotEmpty()) {
            for (linea in lLineas) {
                sCodigo = linea.codigo
                sDescr = linea.descripcion
                sCajas = linea.cajas
                sCant = linea.cantidad
                result.append(fMargenIzq).append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                    .append(ajustarCadena(sDescr, lDescr, true))
                var dCajas = 0.0
                var dCant = 0.0
                if (sCajas != "") dCajas = sCajas.toDouble()
                if (sCant != "") dCant = sCant.toDouble()
                sumaCant += dCant
                sumaCajas += dCajas
                sCajas = String.format(fFtoCant, dCajas)
                sCant = String.format(fFtoCant, dCant)
                result.append(" ").append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                    .append(ajustarCadena(sCant, lCant, false))
                result.append(ccSaltoLinea)
            }
        }

        sCajas = String.format(fFtoCant, sumaCajas)
        sCant = String.format(fFtoCant, sumaCant)
        result.append(ccSaltoLinea)
        result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 42))
            .append(ajustarCadena(sCajas, lCajas, false)).append(stringOfChar(" ", 1))
            .append(ajustarCadena(sCant, lCant, false))
        result.append(stringOfChar(ccSaltoLinea, 15))
        try {
            os.write(result.toString().toByteArray())
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (e: Exception) {
            //
        }
    }


}