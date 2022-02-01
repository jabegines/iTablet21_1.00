package es.albainformatica.albamobileandroid.impresion_informes

import android.annotation.SuppressLint
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import android.content.SharedPreferences
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
import es.albainformatica.albamobileandroid.dao.DocsCabPiesDao
import es.albainformatica.albamobileandroid.dao.TiposIncDao
import es.albainformatica.albamobileandroid.entity.CargasEnt
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

class ImprGenerica(contexto: Context): Runnable {
    val tiposIncDao: TiposIncDao? = getInstance(contexto)?.tiposIncDao()

    private var fContexto: Context = contexto
    private var fDocumento: Documento = Comunicador.fDocumento
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private var fFormasPago: FormasPagoClase = FormasPagoClase(contexto)
    private var fPendiente: PendienteClase = PendienteClase(contexto)
    private lateinit var prefs: SharedPreferences

    private var fFtoCant: String = ""
    private var fFtoPrBase: String = ""
    private var fFtoImpBase: String = ""
    private var fFtoImpII: String = ""
    private var fVtaIvaIncluido: Boolean = false

    private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mBluetoothDevice: BluetoothDevice

    var fTerminado = false
    var fImprimiendo = false
    private var queImprimir: Short = 1
    private var fCargaId = 0
    private val fMargenIzq = ""
    private var fImpresora: Int = IMPRESORA_GENERICA_110
    private var anchoPapel: Short = 69

    private val ccSaltoLinea = "\n"
    private val ccDobleAncho: String = 29.toChar().toString()+ (33.toChar()).toString() + 16.toChar()
    private val ccNormal: String = 29.toChar().toString()+ (33.toChar()).toString() + 0.toChar()
    private val fImprimirDocumento: Short = 1
    private val fImprimirCarga: Short = 2


    init {
        // Obtenemos el documento actual a través del comunicador.
        fImpresora = fConfiguracion.impresora()
        if (fImpresora == IMPRESORA_GENERICA_80) anchoPapel = 48
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
        // Intentamos conectar con Bluetooth. Para ello pasamos la dirección de la impresora.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(fContexto, "No se pudo conectar con Bluetooth", Toast.LENGTH_SHORT)
                .show()
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                fContexto.startActivity(enableBtIntent)
            } else {
                // Leemos la dirección de la impresora Bluetooth de las preferencias.
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
                // hilo, se ejecutará el método run() de la actividad.
            }
        }
    }

    fun imprimirCarga(queCarga: Int) {
        // Intentamos conectar con Bluetooth. Para ello pasamos la dirección de la impresora.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(fContexto, "No se pudo conectar con Bluetooth", Toast.LENGTH_SHORT)
                .show()
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                fContexto.startActivity(enableBtIntent)
            } else {
                // Leemos la dirección de la impresora Bluetooth de las preferencias.
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
                // hilo, se ejecutará el método run() de la actividad.
            }
        }
    }

    private fun inicializarControles() {
        fFtoCant = fConfiguracion.formatoDecCantidad()
        fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoImpII = fConfiguracion.formatoDecImptesIva()
        fVtaIvaIncluido = fConfiguracion.ivaIncluido(fDocumento.fEmpresa)

        // Leemos las preferencias de la aplicación;
        prefs = PreferenceManager.getDefaultSharedPreferences(fContexto)
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
        } catch (ignored: IOException) {
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

                    // Llamo a destruir porque he comprobado que la clase no pasa por el método onDestroy() (supongo que porque
                    // no hereda de Activity), así me aseguro de cerrar el socket y los demás objetos abiertos.
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
                    texto =
                        if (fImpresora == IMPRESORA_GENERICA_110) texto + imprDatosClteYDoc() else texto + imprDatClteDoc80()
                    os.write(stringABytes(texto))
                    texto = ""
                    // Pausa.
                    SystemClock.sleep(500)
                    texto =
                        if (fImpresora == IMPRESORA_GENERICA_110) texto + imprCabLineas() else texto + imprCabLin80()
                    os.write(stringABytes(texto))
                    if (fImpresora == IMPRESORA_GENERICA_110) {
                        imprLineasPositivas(os)
                        imprLineasNegativas(os)
                        imprCabPie(os)
                        imprBases(os)
                    } else {
                        imprLineasPosit80(os)
                        imprLineasNeg80(os)
                        imprCabPie80(os)
                        imprBases80(os)
                    }
                    texto = ""
                    // Nueva pausa.
                    SystemClock.sleep(500)
                    if (fDocumento.fTipoDoc == TIPODOC_FACTURA) {
                        texto += imprFPago()
                        SystemClock.sleep(500)
                    }
                    texto += imprPie()
                    os.write(texto.toByteArray())

                    // Llamo a destruir porque he comprobado que la clase no pasa por el método onDestroy() (supongo que porque
                    // no hereda de Activity), así me aseguro de cerrar el socket y los demás objetos abiertos.
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
            result + fMargenIzq + "Inversión del sujeto pasivo" + ccSaltoLinea
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

    private fun imprBases80(os: OutputStream) {
        var texto = StringBuilder()
        var sBruto: String
        var sImpDto: String
        var sBase: String
        var sPorcIva: String
        var sImpIva: String
        val sTotal: String
        val lBruto = 9
        val lImpDto = 8
        val lBase = 9
        val lImpIva = 8
        val lTotal = 9
        var fHayRecargo = false
        try {
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
                    if (x.fImporteRe != 0.0) fHayRecargo = true
                    texto.append(fMargenIzq).append(ajustarCadena(sBruto, lBruto, false))
                        .append(stringOfChar(" ", 1)).append(ajustarCadena(sImpDto, lImpDto, false))
                        .append(stringOfChar(" ", 1)).append(ajustarCadena(sBase, lBase, false))
                        .append(stringOfChar(" ", 2)).append(ajustarCadena(sPorcIva, 7, false))
                        .append(stringOfChar(" ", 1)).append(ajustarCadena(sImpIva, lImpIva, false))
                        .append(stringOfChar(" ", 1))
                    texto.append(ccSaltoLinea)
                }
            }
            os.write(texto.toString().toByteArray())
            texto = StringBuilder()

            // Imprimimos ahora los recargos de equivalencia
            if (fHayRecargo) imprRecEquiv80(os)
            sTotal = String.format(fFtoImpII, fDocumento.fBases.totalConImptos)
            texto.append(ccSaltoLinea).append(ccSaltoLinea)
            texto.append(stringOfChar(" ", 5)).append("TOTAL IMPORTE: ").append(ccDobleAncho)
                .append(ajustarCadena(sTotal, lTotal, false)).append(
                    ccNormal
            ).append("  Euros").append(ccSaltoLinea)
            os.write(texto.toString().toByteArray())
            // Pausa
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
        }
    }

    private fun imprRecEquiv80(os: OutputStream) {
        val texto = StringBuilder()
        var sPorcRe: String
        var sImpRe: String
        val lImpRe = 7
        texto.append(ccSaltoLinea)
        try {
            for (x in fDocumento.fBases.fLista) {
                if (x.fBaseImponible != 0.0) {
                    sPorcRe = String.format(fFtoImpBase, x.fPorcRe)
                    sImpRe = String.format(fFtoImpBase, x.fImporteRe)
                    if (x.fImporteRe != 0.0) {
                        texto.append("% Rec. equ.: ").append(ajustarCadena(sPorcRe, 5, false))
                        texto.append(stringOfChar(" ", 3)).append("Rec. equ.: ")
                            .append(ajustarCadena(sImpRe, lImpRe, false)).append(
                                ccSaltoLinea
                        )
                    }
                }
            }
            os.write(texto.toString().toByteArray())
        } catch (ignored: Exception) {
        }
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
        val lImpRe = 7
        val lTotal = 8
        try {
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
                    sPorcRe = String.format(fFtoImpBase, x.fPorcRe)
                    sImpRe = String.format(fFtoImpBase, x.fImporteRe)
                    texto.append(fMargenIzq).append(ajustarCadena(sBruto, lBruto, false))
                        .append(stringOfChar(" ", 1)).append(ajustarCadena(sImpDto, lImpDto, false))
                        .append(stringOfChar(" ", 1)).append(ajustarCadena(sBase, lBase, false))
                        .append(stringOfChar(" ", 2)).append(ajustarCadena(sPorcIva, 7, false))
                        .append(stringOfChar(" ", 1)).append(ajustarCadena(sImpIva, lImpIva, false))
                        .append(stringOfChar(" ", 1))
                    if (x.fImporteRe != 0.0) texto.append(ajustarCadena(sPorcRe, 8, false))
                        .append(stringOfChar(" ", 3)).append(
                        ajustarCadena(
                            sImpRe,
                            lImpRe,
                            false
                        )
                    ) else texto.append(stringOfChar(" ", 6 + lImpRe))
                    texto.append(ccSaltoLinea)
                }
            }
            sTotal = String.format(fFtoImpII, fDocumento.fBases.totalConImptos)
            texto.append(ccSaltoLinea).append(ccSaltoLinea)
            texto.append(stringOfChar(" ", 27)).append("TOTAL IMPORTE: ").append(ccDobleAncho)
                .append(ajustarCadena(sTotal, lTotal, false)).append(
                    ccNormal
            ).append("  Euros").append(ccSaltoLinea)
            os.write(texto.toString().toByteArray())
            // Pausa
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
        }
    }

    @SuppressLint("Range")
    private fun imprLineasNeg80(os: OutputStream) {
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
        val lDescr = 18
        val lCajas = 6
        val lCant = 6
        val lPrecio = 6
        val lImpte = 7
        val lLote = 20
        var sumaCajas = 0.0
        var sumaCant = 0.0

        for (linea in fDocumento.lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sCajas = linea.cajas.replace(",", ".")
            sCant = linea.cantidad.replace(",", ".")
            val dCant = sCant.toDouble()
            if (dCant < 0.0) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII.replace(",", ".")
                    sImpte = linea.importeII.replace(",", ".")
                } else {
                    sPrecio = linea.precio.replace(",", ".")
                    sImpte = linea.importe.replace(",", ".")
                }
                result.append(fMargenIzq).append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                    .append(ajustarCadena(sDescr, lDescr, true))
                val dCajas = sCajas.toDouble()
                sumaCant += dCant
                sumaCajas += dCajas

                //sCajas = String.format(fFtoCant, dCajas);
                sCant = String.format(fFtoCant, dCant)
                val dPrecio = sPrecio.toDouble()
                val dImpte = sImpte.toDouble()
                sPrecio = String.format(fFtoPrBase, dPrecio)
                sImpte = String.format(fFtoImpBase, dImpte)
                result.append(" ").append(ajustarCadena(sCant, lCant, false)).append(" ")
                    .append(ajustarCadena(sPrecio, lPrecio, false)).append(" ")
                    .append(ajustarCadena(sImpte, lImpte, false))
                result.append(ccSaltoLinea)

                // Si la línea tiene número de lote lo imprimimos.
                if (linea.lote != "") {
                    sLote = linea.lote
                    result.append(fMargenIzq).append("Numero lote: ").append(ajustarCadena(sLote, lLote, true))
                    result.append(ccSaltoLinea)
                }

                // Si la línea tiene incidencia la imprimimos
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
            result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 13))
                .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                .append(ajustarCadena(sCant, lCant, false)).append(
                    ccSaltoLinea
            )
        }
        try {
            os.write(stringABytes(result.toString()))
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
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
        val lDescr = 29
        val lCajas = 6
        val lCant = 7
        val lPrecio = 7
        val lImpte = 8
        val lLote = 20
        var sumaCajas = 0.0
        var sumaCant = 0.0

        for (linea in fDocumento.lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sCajas = linea.cajas.replace(",", ".")
            sCant = linea.cantidad.replace(",", ".")
            val dCant = sCant.toDouble()
            if (dCant < 0.0) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII.replace(",", ".")
                    sImpte = linea.importeII.replace(",", ".")
                } else {
                    sPrecio = linea.precio.replace(",", ".")
                    sImpte = linea.importe.replace(",", ".")
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

                // Si la línea tiene número de lote lo imprimimos.
                if (linea.lote != "") {
                    sLote = linea.lote
                    result.append(fMargenIzq).append("Numero lote: ").append(ajustarCadena(sLote, lLote, true))
                    result.append(ccSaltoLinea)
                }
                // Si la línea tiene incidencia la imprimimos
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
            result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 31))
                .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                .append(ajustarCadena(sCant, lCant, false)).append(
                    ccSaltoLinea
            )
        }
        try {
            os.write(result.toString().toByteArray())
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
        }
    }

    @SuppressLint("Range")
    private fun imprLineasPosit80(os: OutputStream) {
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
        val lDescr = 18
        val lCajas = 6
        val lCant = 6
        val lPrecio = 6
        val lImpte = 7
        val lLote = 20
        var sumaCajas = 0.0
        var sumaCant = 0.0

        for (linea in fDocumento.lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sCajas = linea.cajas.replace(",", ".")
            sCant = linea.cantidad.replace(",", ".")
            val dCant = sCant.toDouble()
            if (dCant >= 0.0) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII.replace(",", ".")
                    sImpte = linea.importeII.replace(",", ".")
                } else {
                    sPrecio = linea.precio.replace(",", ".")
                    sImpte = linea.importe.replace(",", ".")
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

                // Si la línea es sin cargo lo indicamos
                if (linea.flag and FLAGLINEAVENTA_SIN_CARGO > 0) {
                    sPrecio = "SIN"
                    sImpte = "CARGO"
                } else {
                    sPrecio = String.format(fFtoPrBase, dPrecio)
                    sImpte = String.format(fFtoImpBase, dImpte)
                }
                result.append(" ").append(ajustarCadena(sCant, lCant, false)).append(" ")
                    .append(ajustarCadena(sPrecio, lPrecio, false)).append(" ")
                    .append(ajustarCadena(sImpte, lImpte, false))
                result.append(ccSaltoLinea)

                // Si la línea tiene cajas las imprimimos
                if (linea.cajas.toDouble() != 0.0) {
                    result.append(fMargenIzq).append("Cajas: ").append(ajustarCadena(sCajas, lCajas, false)).append(ccSaltoLinea)
                }
                // Si la línea tiene descuento lo imprimimos
                if (linea.dto.toDouble() != 0.0) {
                    sDto = linea.dto
                    result.append(fMargenIzq).append("% dto.: ").append(ajustarCadena(sDto, 5, false)).append(ccSaltoLinea)
                }
                // Si la línea tiene número de lote lo imprimimos.
                if (linea.lote != "") {
                    sLote = linea.lote
                    result.append(fMargenIzq).append("Numero lote: ").append(ajustarCadena(sLote, lLote, true))
                    result.append(ccSaltoLinea)
                }
                // Si la línea tiene incidencia la imprimimos
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
        // Pausa.
        SystemClock.sleep(200)
        sCajas = String.format(fFtoCant, sumaCajas)
        sCant = String.format(fFtoCant, sumaCant)
        result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 13))
            .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
            .append(ajustarCadena(sCant, lCant, false)).append(
                ccSaltoLinea
        )
        val lineaSimple = StringBuilder(fMargenIzq)
        for (x in 0 until anchoPapel) {
            lineaSimple.append("-")
        }
        result.append(lineaSimple).append(ccSaltoLinea).append(ccSaltoLinea)
        try {
            os.write(stringABytes(result.toString()))
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
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
        val lDescr = 29
        val lCajas = 6
        val lCant = 7
        val lPrecio = 7
        val lImpte = 8
        val lLote = 20
        var sumaCajas = 0.0
        var sumaCant = 0.0

        for (linea in fDocumento.lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sCajas = linea.cajas.replace(",", ".")
            sCant = linea.cantidad.replace(",", ".")
            val dCant = sCant.toDouble()
            if (dCant >= 0.0) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII.replace(",", ".")
                    sImpte = linea.importeII.replace(",", ".")
                } else {
                    sPrecio = linea.precio.replace(",", ".")
                    sImpte = linea.importe.replace(",", ".")
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

                // Si la línea es sin cargo lo indicamos
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

                // Si la línea tiene número de lote lo imprimimos.
                if (linea.lote != "") {
                    sLote = linea.lote
                    result.append(fMargenIzq).append("Numero lote: ")
                        .append(ajustarCadena(sLote, lLote, true))
                    result.append(ccSaltoLinea)
                }
                // Si la línea tiene incidencia la imprimimos
                if (linea.tipoIncId > 0) {
                    fIncidencia = linea.tipoIncId
                    val queDescrInc = tiposIncDao?.dimeDescripcion(fIncidencia) ?: ""
                    if (queDescrInc != "") {
                        result.append(fMargenIzq).append("Incidencia: ").append(fIncidencia).append(" ").append(queDescrInc)
                        result.append(ccSaltoLinea)
                    }
                }
                // Si la línea tiene descuento lo imprimimos
                if (linea.dto != "") {
                    sDto = linea.dto
                    result.append(fMargenIzq).append("% dto.: ").append(ajustarCadena(sDto, 5, false))
                    result.append(ccSaltoLinea)
                }
            }
        }
        result.append(ccSaltoLinea)
        sCajas = String.format(fFtoCant, sumaCajas)
        sCant = String.format(fFtoCant, sumaCant)
        result.append(fMargenIzq).append("SUMAS: ").append(stringOfChar(" ", 31))
            .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
            .append(ajustarCadena(sCant, lCant, false)).append(
                ccSaltoLinea
        )
        val lineaSimple = StringBuilder(fMargenIzq)
        for (x in 0 until anchoPapel) {
            lineaSimple.append("-")
        }
        result.append(lineaSimple).append(ccSaltoLinea).append(ccSaltoLinea)
        try {
            os.write(result.toString().toByteArray())
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
        }
    }

    private fun imprCabPie80(os: OutputStream) {
        var texto = ""
        val lineaSimple = StringBuilder(fMargenIzq)
        try {
            texto += stringOfChar(ccSaltoLinea, 2)
            os.write(texto.toByteArray())
            for (x in 0 until anchoPapel) {
                lineaSimple.append("-")
            }
            texto = lineaSimple.toString() + ccSaltoLinea
            texto = (texto + fMargenIzq + stringOfChar(" ", 4) + "VENTA" + stringOfChar(
                " ",
                5
            ) + "DTO" + stringOfChar(" ", 5)
                    + "NETO" + stringOfChar(" ", 4) + "%IVA" + stringOfChar(
                " ",
                5
            ) + "IVA" + ccSaltoLinea)
            texto = texto + lineaSimple + ccSaltoLinea
            os.write(texto.toByteArray())
        } catch (ignored: Exception) {
        }
        // Pausa
        SystemClock.sleep(500)
    }

    private fun imprCabPie(os: OutputStream) {
        var texto = ""
        val lineaSimple = StringBuilder(fMargenIzq)
        try {
            texto += stringOfChar(ccSaltoLinea, 2)
            os.write(texto.toByteArray())
            for (x in 0 until anchoPapel) {
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
        } catch (ignored: Exception) {
        }
        // Pausa
        SystemClock.sleep(500)
    }

    private fun imprCabLin80(): String {
        val lineaSimple = StringBuilder(fMargenIzq)
        var result: String =
            fMargenIzq + "COD." + stringOfChar(" ", 4) + "ARTICULO" + stringOfChar(" ", 12) +
                "UNID" + stringOfChar(" ", 2) + "PRECIO" + stringOfChar(
            " ",
            3
        ) + "TOTAL" + ccSaltoLinea
        for (x in 0 until anchoPapel) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        return result
    }

    private fun imprCabLineas(): String {
        val lineaSimple = StringBuilder(fMargenIzq)
        var result: String = (fMargenIzq + "COD." + stringOfChar(" ", 4) + "ARTICULO" + stringOfChar(
            " ",
            23
        ) + "CAJAS"
                + stringOfChar(" ", 4) + "UNID" + stringOfChar(" ", 2) + "PRECIO"
                + stringOfChar(" ", 4) + "TOTAL" + ccSaltoLinea)
        for (x in 0 until anchoPapel) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        return result
    }

    private fun imprDatClteDoc80(): String {
        var result: String
        val sLongDatosClte = anchoPapel
        result = ajustarCadena(fMargenIzq + ponerCeros(fDocumento.fClientes.fCodigo, ancho_codclte) + " " +
                fDocumento.fClientes.fNombre, sLongDatosClte.toInt(), true) + ccSaltoLinea
        result = result + ajustarCadena(fMargenIzq + fDocumento.fClientes.fNomComercial,
            sLongDatosClte.toInt(),
            true
        ) + ccSaltoLinea
        result = result + ajustarCadena(fMargenIzq + fDocumento.fClientes.fDireccion, sLongDatosClte.toInt(), true) + ccSaltoLinea
        result = result + ajustarCadena(fMargenIzq + fDocumento.fClientes.fCodPostal + " " + fDocumento.fClientes.fPoblacion, sLongDatosClte.toInt(), true) + ccSaltoLinea
        result = result + ajustarCadena(fMargenIzq + fDocumento.fClientes.fProvincia, sLongDatosClte.toInt(), true) + ccSaltoLinea
        result = result + ajustarCadena(fMargenIzq + "C.I.F.: " + fDocumento.fClientes.fCif, sLongDatosClte.toInt(), true) + ccSaltoLinea
        result += ccSaltoLinea
        result = result + "Vendedor: " + fConfiguracion.vendedor() + " " + fConfiguracion.nombreVendedor() + ccSaltoLinea
        result =
            result + "Fecha: " + fDocumento.fFecha + "     Hora: " + fDocumento.fHora + ccSaltoLinea
        result = result + "Documento: " + tipoDocAsString(fDocumento.fTipoDoc)
        result = result + "     Numero: " + fDocumento.serie + "/" + fDocumento.numero
        result = result + ccSaltoLinea + ccSaltoLinea + ccSaltoLinea
        return result
    }

    private fun imprDatosClteYDoc(): String {
        var result: String
        var cCadena: String
        val sLongDatosClte: Short = 47
        cCadena = ajustarCadena(fMargenIzq + ponerCeros(fDocumento.fClientes.fCodigo.toString(), ancho_codclte) + " " +
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
        // Si la cadena supera el máximo de caracteres, la recortamos. En cambio, si no llega a esta cifra, le añadimos espacios al final.
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

        // En albaranes imprimimos las observaciones del documento.
        if (fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            result.append(fDocumento.fObs1)
            result.append(ccSaltoLinea)
            result.append(fDocumento.fObs2)
        }
        for (x in 1..5) {
            result.append(ccSaltoLinea)
        }
        return result.toString()
    }

    private fun imprCabecera(): String {
        val docsCabPiesDao: DocsCabPiesDao? = getInstance(fContexto)?.docsCabPiesDao()
        val cabeceraDoc = docsCabPiesDao?.cabeceraDoc(fDocumento.fEmpresa) ?: ""
        val lineasCabDoc =
            fConfiguracion.lineasCabDocConMargIzq(fDocumento.fEmpresa, fMargenIzq)
        val result = StringBuilder(fMargenIzq)
        val lineasDobles = StringBuilder()
        for (x in 0 until anchoPapel) {
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

        if (lLineas.count() > 0) {
            for (linea in lLineas) {
                sCodigo = linea.codigo
                sDescr = linea.descripcion
                sCajas = linea.cajas.replace(",", ".")
                sCant = linea.cantidad.replace(",", ".")
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
            .append(ajustarCadena(sCajas, lCajas, false))
            .append(stringOfChar(" ", 1)).append(ajustarCadena(sCant, lCant, false))
        result.append(stringOfChar(ccSaltoLinea, 15))
        try {
            os.write(result.toString().toByteArray())
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (ignored: Exception) {
        }
    }


}