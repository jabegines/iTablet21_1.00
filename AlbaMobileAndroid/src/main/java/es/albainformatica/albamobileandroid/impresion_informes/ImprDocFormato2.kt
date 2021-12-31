package es.albainformatica.albamobileandroid.impresion_informes

import android.annotation.SuppressLint
import es.albainformatica.albamobileandroid.stringABytes
import es.albainformatica.albamobileandroid.StringOfChar
import es.albainformatica.albamobileandroid.tipoDocAsString
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.Configuracion
import android.content.SharedPreferences
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.content.Context
import es.albainformatica.albamobileandroid.Comunicador
import android.preference.PreferenceManager
import android.widget.Toast
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * Created by jabegines on 18/08/2017.
 */
class ImprDocFormato2(contexto: Context): Runnable {
    private var fContexto: Context = contexto
    private var fDocumento: Documento = Comunicador.fDocumento
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private lateinit var pref: SharedPreferences

    private var fFtoCant: String = ""
    private var fNumLineas = 0
    private var fTotalCajas = 0.0

    private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mBluetoothDevice: BluetoothDevice

    private val ccSaltoLinea = "\n"
    private val ccDobleAncho = 14.toChar().toString()
    private val ccNormal = 20.toChar().toString()


    init {
        // Obtenemos el documento actual a través del comunicador.
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
        fNumLineas = 0

        // Leemos las preferencias de la aplicación;
        pref = PreferenceManager.getDefaultSharedPreferences(fContexto)
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
                val mDeviceAddress = pref.getString("impresoraBT", "")
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress)
                mBluetoothConnectProgressDialog = ProgressDialog.show(
                    fContexto,
                    "Conectando...",
                    mBluetoothDevice.name,
                    true,
                    false
                )
                val mBlutoothConnectThread = Thread(this)
                mBlutoothConnectThread.start()
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
            imprimirDoc()
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

    private fun imprimirDoc() {
        val t: Thread = object : Thread() {
            override fun run() {
                var texto = ""
                fTotalCajas = 0.0
                try {
                    val os = mBluetoothSocket.outputStream
                    if (fDocumento.fAplicarIva) texto = imprCabecera() else fNumLineas = -5
                    texto += imprDatosClteYDoc()
                    os.write(stringABytes(texto))
                    texto = ""
                    // Pausa.
                    SystemClock.sleep(500)
                    texto += imprCabLineas()
                    os.write(stringABytes(texto))
                    texto = ""
                    imprLineas(os)
                    texto += imprPie()
                    os.write(stringABytes(texto))

                    // Llamo a destruir porque he comprobado que la clase no pasa por el método onDestroy() (supongo que porque
                    // no hereda de Activity), así me aseguro de cerrar el socket y los demás objetos abiertos.
                    destruir()
                } catch (e: Exception) {
                    //
                }
            }
        }
        t.start()
    }

    @SuppressLint("Range")
    private fun imprLineas(os: OutputStream) {
        var result = StringBuilder()
        var sCodigo: String
        var sDescr: String
        var sCajas: String
        var sCant: String
        val lCodigo = 7
        val lDescr = 20
        val lCant = 30
        fNumLineas = 17
        fDocumento.cLineas.moveToFirst()
        while (!fDocumento.cLineas.isAfterLast) {
            sCodigo = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("codigo"))
            sDescr = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("descr"))
            sCajas = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cajas"))
            sCant = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cantidad"))
            result.append(ajustarCadena(sCodigo, lCodigo, true)).append(" ")
                .append(ajustarCadena(sDescr, lDescr, true))
            val dCajas = sCajas.toDouble()
            fTotalCajas += dCajas
            val dCant = sCant.toDouble()
            sCant = String.format(fFtoCant, dCant)
            result.append(" ").append(ajustarCadena(sCant, lCant, false)).append(" ")
            result.append(ccSaltoLinea)
            fNumLineas++
            try {
                os.write(result.toString().toByteArray())
                result = StringBuilder()
                // Hacemos una pausa para agilizar los buffers de las impresoras
                SystemClock.sleep(500)
            } catch (e: Exception) {
                //
            }
            fDocumento.cLineas.moveToNext()
        }
    }

    private fun imprCabLineas(): String {
        val lineaSimple = StringBuilder()
        var result: String = "COD." + StringOfChar(" ", 4) + "ARTICULO" + StringOfChar(
            " ",
            35
        ) + "CANTIDAD" + ccSaltoLinea
        for (x in 0..59) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        return result
    }

    private fun imprDatosClteYDoc(): String {
        var result: String
        var cCadena: String = ajustarCadena(
            fDocumento.fClientes.getCodigo() + " "
                    + fDocumento.fClientes.getNFiscal(), 35, true
        )
        result = cCadena
        result += ccSaltoLinea
        cCadena = ajustarCadena(fDocumento.fClientes.getDireccion(), 35, true)
        result += cCadena
        result = (result + StringOfChar(" ", 5) + "Hora: "
                + StringOfChar(" ", 4) + fDocumento.fHora)
        result += ccSaltoLinea
        result = (result
                + ajustarCadena(
            fDocumento.fClientes.getCodPostal() + " "
                    + fDocumento.fClientes.getPoblacion(), 35, true
        ))
        result = (result + StringOfChar(" ", 5) + "Fecha: "
                + StringOfChar(" ", 3) + fDocumento.fFecha)
        result += ccSaltoLinea
        result += ajustarCadena(fDocumento.fClientes.getProvincia(), 35, true)
        result = result + StringOfChar(" ", 5) + "Doc: " + tipoDocAsString(fDocumento.fTipoDoc)
        result += ccSaltoLinea
        result += ajustarCadena("C.I.F.: " + fDocumento.fClientes.getCIF(), 35, true)
        result = result + StringOfChar(
            " ",
            5
        ) + "Num.: " + fDocumento.serie + "/" + fDocumento.numero
        result = result + ccSaltoLinea + ccSaltoLinea
        return result
    }

    private fun ajustarCadena(
        cCadena: String, maxLong: Int, fPorLaDerecha: Boolean
    ): String {
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

    private fun imprPie(): String {
        val result: StringBuilder
        val lineaSimple = StringBuilder()
        val sTotalCajas = fTotalCajas.toString()
        result = StringBuilder(
            ccSaltoLinea + "Total Cajas: " + sTotalCajas + StringOfChar(
                " ",
                5
            ) + "Recibi conforme: El cliente" + ccSaltoLinea
        )
        result.append(ccSaltoLinea).append(ccSaltoLinea)
        fNumLineas += 4
        result.append("Mercancia entregada por distribuidor:").append(ccSaltoLinea)
        fNumLineas += 1
        for (x in 0..59) {
            lineaSimple.append("-")
        }
        result.append(lineaSimple).append(ccSaltoLinea)
        result.append(ccDobleAncho)
        result.append("FRC. RODRIGUEZ MARQUEZ, S.L.").append(ccSaltoLinea)
        result.append(ccNormal)
        result.append("P.I. CANTAELGALLO, 26 AMPLIACION N6 28").append(ccSaltoLinea)
        result.append("21200 - ARACENA (HUELVA)").append(ccSaltoLinea)
        result.append("CIF: B-21395041    TLF: 616007900 / 696962951")

        // Dejamos el valor de fNumLineas en número entre 0 y 49, que serán las líneas que tenemos que avanzar para el corte del papel.
        if (fNumLineas > 49) {
            while (fNumLineas > 49) {
                fNumLineas -= 49
            }
            fNumLineas -= 2
        }

        // Avanzamos el papel hasta el punto de corte.
        for (x in fNumLineas..48) {
            result.append(ccSaltoLinea)
        }
        return result.toString()
    }

    private fun imprCabecera(): String {
        var result = ""
        val lineasDobles = StringBuilder()
        for (x in 0..59) {
            lineasDobles.append("=")
        }
        result = result + lineasDobles + ccSaltoLinea
        result = "$result     MERCANCIA ENTREGADA"
        result = result + "A FACTURAR POR FRONERI IBERIA" + ccSaltoLinea
        result = result + lineasDobles + ccSaltoLinea + ccSaltoLinea
        return result
    }

}