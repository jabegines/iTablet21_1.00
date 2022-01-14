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
import android.preference.PreferenceManager
import android.widget.Toast
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import es.albainformatica.albamobileandroid.*
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class ImprimirDocumento(contexto: Context): Runnable {
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
    private var fPrimLinArticulos = 0
    private var fPosicionPie = 0
    private var fLineasImpresas = 0

    private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mBluetoothDevice: BluetoothDevice

    var fTerminado = false
    var fImprimiendo = false
    private var queImprimir: Short = 1
    private var fImprSinValorar = false
    private var fCargaId = 0

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

    private fun inicializarControles() {
        fFtoCant = fConfiguracion.formatoDecCantidad()
        fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoImpII = fConfiguracion.formatoDecImptesIva()
        fLineasImpresas = 0

        // Leemos las preferencias de la aplicación;
        prefs = PreferenceManager.getDefaultSharedPreferences(fContexto)
        fNumLineasDoc = prefs.getString("lineas_doc", "48")?.toInt() ?: 48
        fPrimeraLinea = prefs.getString("primera_linea", "8")?.toInt() ?: 8
        fPosicionCorte = prefs.getString("posicion_corte", "8")?.toInt() ?: 8
        fPrimLinArticulos = prefs.getString("prim_linea_articulos", "24")?.toInt() ?: 24
        fPosicionPie = prefs.getString("posicion_pie", "37")?.toInt() ?: 37
    }

    fun imprimir(sinValorar: Boolean) {
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
                fImprSinValorar = sinValorar
                fVtaIvaIncluido =
                    fConfiguracion.ivaIncluido(fDocumento.fEmpresa.toString().toInt())
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
                    os.write(stringABytes(texto))
                    // Pausa
                    SystemClock.sleep(500)
                    fLineasImpresas = 8 + fPosicionCorte
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
                    os.write(stringABytes(texto))
                    texto = ""
                    // Pausa.
                    SystemClock.sleep(500)
                    texto += imprDatosClteYDoc()
                    os.write(stringABytes(texto))
                    texto = ""
                    // Pausa.
                    SystemClock.sleep(500)
                    texto += imprCabLineas()
                    os.write(stringABytes(texto))
                    imprLineas(os)
                    if (!fImprSinValorar) {
                        imprCabPie(os)
                        imprBases(os)
                    }
                    texto = ""
                    // Nueva pausa.
                    SystemClock.sleep(500)
                    if (fDocumento.fTipoDoc == TIPODOC_FACTURA) {
                        texto += imprFPago()
                    }
                    texto += imprPie()
                    os.write(stringABytes(texto))

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
        var lineaSimple = StringBuilder()
        val lImptes = 9
        var result = ccSaltoLinea
        if (fDocumento.fSerieExenta) result =
            result + "Inversión del sujeto pasivo" + ccSaltoLinea
        result = result + "Forma de pago: " + fFormasPago.getDescrFPago(fDocumento.fPago)
        result += ccSaltoLinea
        if (fPendiente.abrirDocumento()) {
            for (x in 0..34) {
                lineaSimple.append("-")
            }
            result = result + lineaSimple + ccSaltoLinea
            result = (result + StringOfChar(" ", 2) + "IMPORTE" + StringOfChar(" ", 2) + "ENTREGADO"
                    + StringOfChar(" ", 2) + "PENDIENTE" + ccSaltoLinea)
            lineaSimple = StringBuilder()
            for (x in 0..34) {
                lineaSimple.append("-")
            }
            result = result + lineaSimple + ccSaltoLinea
            val dImporte = fPendiente.importe.toDouble()
            sImpte = String.format(fFtoImpII, dImporte)
            val dCobrado = fPendiente.cobrado.toDouble()
            sCobrado = String.format(fFtoImpII, dCobrado)
            val dPdte = dImporte - dCobrado
            sPdte = String.format(fFtoImpII, dPdte)
            result = (result + ajustarCadena(sImpte, lImptes, false) + StringOfChar(" ", 2)
                    + ajustarCadena(sCobrado, lImptes, false) + StringOfChar(
                " ",
                2
            ) + ajustarCadena(sPdte, lImptes, false))
            result += ccSaltoLinea
        }
        fLineasImpresas =
            if (fDocumento.fSerieExenta) fLineasImpresas + 7 else fLineasImpresas + 6
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
        val lBruto = 9
        val lImpDto = 8
        val lBase = 9
        val lImpIva = 9
        val lImpRe = 8
        val lTotal = 9
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
                    texto.append(ajustarCadena(sBruto, lBruto, false)).append(StringOfChar(" ", 1))
                        .append(ajustarCadena(sImpDto, lImpDto, false)).append(StringOfChar(" ", 1))
                        .append(ajustarCadena(sBase, lBase, false)).append(StringOfChar(" ", 2))
                        .append(ajustarCadena(sPorcIva, 5, false)).append(StringOfChar(" ", 1))
                        .append(ajustarCadena(sImpIva, lImpIva, false)).append(StringOfChar(" ", 1))
                    if (x.fImporteRe != 0.0) texto.append(ajustarCadena(sPorcRe, 5, false))
                        .append(StringOfChar(" ", 1)).append(
                        ajustarCadena(
                            sImpRe,
                            lImpRe,
                            false
                        )
                    ) else texto.append(StringOfChar(" ", 6 + lImpRe))
                    texto.append(ccSaltoLinea)
                    fLineasImpresas++
                }
            }
            sTotal = String.format(fFtoImpII, fDocumento.fBases.totalConImptos)
            texto.append(ccSaltoLinea)
            texto.append(StringOfChar(" ", 10)).append("TOTAL IMPORTE: ").append(ccDobleAncho)
                .append(ajustarCadena(sTotal, lTotal, false)).append(
                    ccNormal
            ).append("  Euros").append(ccSaltoLinea)
            fLineasImpresas += 2
            os.write(texto.toString().toByteArray())
            // Pausa
            SystemClock.sleep(500)
        } catch (e: Exception) {
            //
        }
    }

    @SuppressLint("Range")
    private fun imprLineas(os: OutputStream) {
        var result = StringBuilder()
        var sCodigo: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        var sPrecio: String
        var sDto: String
        var sImpte: String
        var sLote: String
        val lCodigo = 7
        val lDescr = 19
        val lCajas = 6
        val lCant = 7
        val lPrecio = 7
        val lDto = 5
        val lImpte = 8
        val lLote = 20
        fLineasImpresas = fPrimLinArticulos

        for (linea in fDocumento.lLineas) {
            sumaYSigue(os)
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            result.append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                .append(ajustarCadena(sDescr, lDescr, true))
            sCajas = linea.cajas
            sCant = linea.cantidad
            val dCajas = sCajas.toDouble()
            val dCant = sCant.toDouble()
            sCajas = String.format(fFtoCant, dCajas)
            sCant = String.format(fFtoCant, dCant)
            result.append(" ").append(ajustarCadena(sCajas, lCajas, false)).append(" ")
                .append(ajustarCadena(sCant, lCant, false)).append(" ")
            if (!fImprSinValorar) {
                if (fVtaIvaIncluido) {
                    sPrecio = linea.precioII
                    sImpte = linea.importeII
                } else {
                    sPrecio = linea.precio
                    sImpte = linea.importe
                }
                val dPrecio = sPrecio.toDouble()
                val dImpte = sImpte.toDouble()
                sPrecio = String.format(fFtoPrBase, dPrecio)
                sImpte = String.format(fFtoImpBase, dImpte)
                result.append(ajustarCadena(sPrecio, lPrecio, false)).append(" ")
                    .append(ajustarCadena(sImpte, lImpte, false))
            }
            result.append(ccSaltoLinea)
            fLineasImpresas++
            var dDto = 0.0
            sDto = ""
            if (linea.dto != "") {
                sDto = linea.dto
                dDto = sDto.toDouble()
            }
            // Si la línea tiene número de lote lo imprimimos. También si tiene descuento por línea.
            if (linea.lote != "") {
                sLote = linea.lote
                result.append("Numero lote: ").append(ajustarCadena(sLote, lLote, true))
                if (dDto != 0.0) result.append(" % Dto.: ").append(ajustarCadena(sDto, lDto, true))
                result.append(ccSaltoLinea)
                fLineasImpresas++
            } else {
                if (dDto != 0.0) {
                    result.append("% Dto.: ").append(ajustarCadena(sDto, lDto, true))
                    result.append(ccSaltoLinea)
                    fLineasImpresas++
                }
            }
            try {
                os.write(stringABytes(result.toString()))
                result = StringBuilder()
                // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
                SystemClock.sleep(500)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun imprCabPie(os: OutputStream) {
        var texto = StringBuilder()
        val lineaSimple = StringBuilder()
        try {
            // Primero nos desplazamos hasta la posición de inicio del pie
            for (x in fLineasImpresas until fPosicionPie) {
                texto.append(ccSaltoLinea)
            }
            os.write(stringABytes(texto.toString()))
            fLineasImpresas = fPosicionPie
            for (x in 0..59) {
                lineaSimple.append("-")
            }
            texto = StringBuilder(lineaSimple.toString() + ccSaltoLinea)
            texto.append(StringOfChar(" ", 3)).append("VENTA").append(StringOfChar(" ", 7))
                .append("DTO")
                .append(StringOfChar(" ", 5)).append("NETO").append(StringOfChar(" ", 4))
                .append("%IVA")
                .append(StringOfChar(" ", 6)).append("IVA").append(StringOfChar(" ", 3))
                .append("%REC")
                .append(StringOfChar(" ", 5)).append("REC").append(ccSaltoLinea)
            texto.append(lineaSimple).append(ccSaltoLinea)
            os.write(stringABytes(texto.toString()))
        } catch (e: Exception) {
            //
        }
        // Pausa
        SystemClock.sleep(500)
        fLineasImpresas = fPosicionPie + 3
    }

    private fun imprCabLineas(): String {
        val lineaSimple = StringBuilder()
        var result: String = "COD." + StringOfChar(" ", 4) + "ARTICULO" + StringOfChar(
            " ",
            13
        ) + "CAJAS" + StringOfChar(" ", 4) + "UNID"
        result = if (!fImprSinValorar) {
            result + StringOfChar(" ", 2) + "PRECIO" + StringOfChar(
                " ",
                4
            ) + "TOTAL" + ccSaltoLinea
        } else {
            result + ccSaltoLinea
        }
        for (x in 0..58) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        return result
    }

    private fun imprDatosClteYDoc(): String {
        var result = ""

        // Imprimimos el nombre comercial de cliente
        if (fConfiguracion.aconsNomComercial()) {
            result = ajustarCadena(fDocumento.fClientes.fNomComercial, 35, true) + ccSaltoLinea
        }
        var cCadena: String = ajustarCadena(ponerCeros(fDocumento.fClientes.fCodigo.toString(), ancho_codclte) + " " +
                fDocumento.fClientes.fNombre, 35, true)
        result += cCadena
        cCadena = StringOfChar(
            " ",
            5
        ) + "Vendedor: " + fConfiguracion.vendedor() // + " " + fConfiguracion.nombreVendedor(), 20, true);
        result += cCadena
        result += ccSaltoLinea
        result += ajustarCadena(fDocumento.fClientes.fDireccion, 35, true)
        cCadena = StringOfChar(" ", 5) + "Hora: " + fDocumento.fHora
        result += cCadena
        result += ccSaltoLinea
        result += ajustarCadena(fDocumento.fClientes.fCodPostal + " " + fDocumento.fClientes.fPoblacion, 35, true)
        cCadena = StringOfChar(" ", 5) + "Fecha: " + fDocumento.fFecha
        result += cCadena
        result += ccSaltoLinea
        result += ajustarCadena(fDocumento.fClientes.fProvincia, 35, true)
        result = result + StringOfChar(" ", 5) + "Doc: " + tipoDocAsString(fDocumento.fTipoDoc)
        result += ccSaltoLinea
        result += ajustarCadena("C.I.F.: " + fDocumento.fClientes.fCif, 35, true)
        result = result + StringOfChar(" ", 5) + "Num.: " + fDocumento.serie + "/" + fDocumento.numero
        result = result + ccSaltoLinea + ccSaltoLinea
        return result
    }

    private fun ajustarCadena(
        cCadena: String, maxLong: Int, fPorLaDerecha: Boolean
    ): String {
        var result = cCadena
        // Si la cadena supera el máximo de caracteres, la recortamos. En cambio, si no llega a esta cifra, le añadimos espacios al final.
        if (result.length > maxLong) result =
            result.substring(0, maxLong) else if (result.length < maxLong) {
            result = if (fPorLaDerecha) result + StringOfChar(
                " ",
                maxLong - result.length
            ) else StringOfChar(" ", maxLong - result.length) + result
        }
        return result
    }

    private fun imprPie(): String {
        val result: StringBuilder
        val lineasPieDoc = fConfiguracion.lineasPieDoc(fDocumento.fEmpresa)
        result = StringBuilder(ccSaltoLinea + lineasPieDoc + ccSaltoLinea)
        fLineasImpresas += 1

        // En albaranes imprimimos las observaciones del documento.
        if (fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            result.append(fDocumento.fObs1)
            result.append(ccSaltoLinea)
            result.append(fDocumento.fObs2)
            fLineasImpresas += 1
        }

        // Avanzamos el papel hasta el punto de corte
        for (x in fLineasImpresas until fNumLineasDoc + fPosicionCorte) {
            result.append(ccSaltoLinea)
        }
        return result.toString()
    }

    private fun sumaYSigue(os: OutputStream) {
        var texto = StringBuilder()
        if (fLineasImpresas >= fPosicionPie) {
            try {
                // Imprimimos la cabecera del pie
                imprCabPie(os)
                // A continuación nos desplazamos hasta el fin del papel
                for (x in fLineasImpresas until fNumLineasDoc) {
                    texto.append(ccSaltoLinea)
                }
                os.write(stringABytes(texto.toString()))
                // Pausa.
                SystemClock.sleep(500)

                // Ahora avanzamos el papel hasta la posición de corte. Luego, en imprCabecera nos situaremos en la primera línea que queramos imprimir.
                texto = StringBuilder()
                fLineasImpresas = 0
                for (x in fLineasImpresas until fPosicionCorte + 1) {
                    texto.append(ccSaltoLinea)
                }
                os.write(stringABytes(texto.toString()))
                // Pausa.
                SystemClock.sleep(500)
                texto = StringBuilder(imprCabecera())
                // Pausa.
                SystemClock.sleep(500)
                texto.append(imprDatosClteYDoc())
                os.write(stringABytes(texto.toString()))
                texto = StringBuilder()
                // Pausa.
                SystemClock.sleep(500)
                texto.append(imprCabLineas())
                os.write(stringABytes(texto.toString()))
                // Establecemos el valor para fLineasImpresas
                fLineasImpresas = fPrimLinArticulos
            } catch (e: Exception) {
                //
            }
        }
    }

    private fun imprCabecera(): String {
        val docsCabPiesDao = myBD?.docsCabPiesDao()
        val cabeceraDoc = docsCabPiesDao?.cabeceraDoc(fDocumento.fEmpresa) ?: ""
        val lineasCabDoc = fConfiguracion.lineasCabDoc(fDocumento.fEmpresa)
        val result = StringBuilder()
        val lineasDobles = StringBuilder()

        // Avanzamos hasta la primera línea de impresión (nos sirve para documentos con la cabecera preimpresa)
        for (x in fPosicionCorte until fPrimeraLinea) {
            result.append(ccSaltoLinea)
        }
        for (x in 0..59) {
            lineasDobles.append("=")
        }
        result.append(lineasDobles).append(ccSaltoLinea)
        result.append(ccDobleAncho)
        result.append(cabeceraDoc).append(ccSaltoLinea)
        result.append(ccNormal)
        result.append(lineasCabDoc).append(ccSaltoLinea)
        result.append(lineasDobles).append(ccSaltoLinea)
        return result.toString()
    }

    @SuppressLint("Range")
    private fun imprCabeceraCarga(): String {
        val result = StringBuilder()
        val lineasDobles = StringBuilder()
        val lineaSimple = StringBuilder()
        fLineasImpresas = fPosicionCorte

        // Avanzamos hasta la primera línea de impresión (nos sirve para documentos con la cabecera preimpresa)
        for (x in fLineasImpresas until fPrimeraLinea) {
            result.append(ccSaltoLinea)
        }
        // TODO
        /*
        val cursor = dbAlba.rawQuery("SELECT * FROM cargas WHERE cargaId = $fCargaId", null)
        if (cursor.moveToFirst()) {
            for (x in 0..59) {
                lineasDobles.append("=")
            }
            result.append("LISTADO DE CARGA").append(ccSaltoLinea).append(ccSaltoLinea)
            result.append("Nueva carga  ")
            result.append(cursor.getString(cursor.getColumnIndex("fecha")))
            result.append(StringOfChar(" ", 3))
                .append(cursor.getString(cursor.getColumnIndex("hora")))
            result.append(ccSaltoLinea)
            result.append("Terminal: ").append(fConfiguracion.codTerminal()).append(" ").append(
                fConfiguracion.nombreTerminal()
            ).append(ccSaltoLinea)
            result.append("Vendedor: ").append(fConfiguracion.vendedor()).append(" ").append(
                fConfiguracion.nombreVendedor()
            ).append(ccSaltoLinea)
            result.append(lineasDobles).append(StringOfChar(ccSaltoLinea, 3))
            result.append("Codigo").append(StringOfChar(" ", 2)).append("Descripcion")
                .append(StringOfChar(" ", 16))
                .append("Lote").append(StringOfChar(" ", 8)).append("Cajas")
                .append(StringOfChar(" ", 3))
                .append("Cant.").append(ccSaltoLinea)
            for (x in 0..59) {
                lineaSimple.append("-")
            }
            result.append(lineaSimple).append(ccSaltoLinea)
        }
        cursor.close()
        */
        return result.toString()
    }

    @SuppressLint("Range")
    private fun imprLineasCarga(os: OutputStream) {
        val result = StringBuilder()
        var sCodigo: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        var sLote: String
        val lCodigo = 6
        val lDescr = 25
        val lLote = 10
        val lCajas = 6
        val lCant = 7
        var sumaCajas = 0.0
        var sumaCant = 0.0
        // TODO
        /*
        val cursor = dbAlba.rawQuery(
            "SELECT A.*, B.codigo, B.descr FROM cargasLineas A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " WHERE A.cargaId = " + fCargaId +
                    " ORDER BY B.codigo", null
        )
        if (cursor.moveToFirst()) {
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                sCodigo = cursor.getString(cursor.getColumnIndex("codigo"))
                sDescr = cursor.getString(cursor.getColumnIndex("descr"))
                sLote = cursor.getString(cursor.getColumnIndex("lote"))
                sCajas = cursor.getString(cursor.getColumnIndex("cajas"))
                sCant = cursor.getString(cursor.getColumnIndex("cantidad"))
                result.append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                    .append(ajustarCadena(sDescr, lDescr, true))
                    .append(StringOfChar(" ", 3)).append(ajustarCadena(sLote, lLote, true))
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
                fLineasImpresas++
                cursor.moveToNext()
            }
        }
        cursor.close()
        */
        sCajas = String.format(fFtoCant, sumaCajas)
        sCant = String.format(fFtoCant, sumaCant)
        result.append(ccSaltoLinea)
        result.append("SUMAS: ").append(StringOfChar(" ", 39))
            .append(ajustarCadena(sCajas, lCajas, false))
            .append(StringOfChar(" ", 1)).append(ajustarCadena(sCant, lCant, false))
        fLineasImpresas += 2

        // Avanzamos el papel hasta el punto de corte
        for (x in fLineasImpresas until fNumLineasDoc + fPosicionCorte - 1) {
            result.append(ccSaltoLinea)
        }
        try {
            os.write(stringABytes(result.toString()))
            // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
            SystemClock.sleep(500)
        } catch (e: Exception) {
            //
        }
    }



}