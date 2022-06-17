package es.albainformatica.albamobileandroid.impresion_informes

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import androidx.preference.PreferenceManager
import android.widget.Toast
import datamaxoneil.connection.ConnectionBase
import datamaxoneil.connection.Connection_Bluetooth
import datamaxoneil.printer.DocumentExPCL_LP
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.StockDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by jabegines on 09/05/2016.
 */
class InfStock(contexto: Context) : Runnable {
    private var fContexto: Context = contexto
    private var prefs: SharedPreferences

    private var fFtoDecCant: String = ""
    private var fElementosImpresos = 0
    private var fAnchoPapel: Short = 0
    private var fImpresora = 0

    private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mBluetoothDevice: BluetoothDevice

    private val CCSALTOLINEA = "\n"



    init {
        val fConfiguracion = Comunicador.fConfiguracion
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fImpresora = fConfiguracion.impresora()
        prefs = PreferenceManager.getDefaultSharedPreferences(fContexto)
        fElementosImpresos = 0
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
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fContexto.startActivity(enableBtIntent)
        } else {
            // Leemos la dirección de la impresora Bluetooth de las preferencias.
            val mDeviceAddress = prefs.getString("impresoraBT", "")
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress)
            mBluetoothConnectProgressDialog = ProgressDialog.show(
                fContexto, "Conectando...", mBluetoothDevice.name, true, false)
            val mBluetoothConnectThread = Thread(this)
            mBluetoothConnectThread.start()
            // Una vez conectados, arrancamos el hilo. Una vez que arrancamos el
            // hilo, se ejecutará el método run() de la actividad.
        }
    }

    override fun run() {
        try {
            if (fImpresora == IMPRESORA_DATAMAX_APEX_2) {
                imprInfDatamaxApex2()
            } else {
                // Obtenemos un bluetoothsocket y lo conectamos. A partir de entonces, llamamos a imprimirDoc().
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
                mBluetoothAdapter.cancelDiscovery()
                mBluetoothSocket.connect()
                mHandler.sendEmptyMessage(0)
                imprimirInforme()
            }
        } catch (eConnectException: IOException) {
            closeSocket(mBluetoothSocket)
        }
    }

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            mBluetoothConnectProgressDialog.dismiss()
        }
    }

    private fun closeSocket(nOpenSocket: BluetoothSocket?) {
        try {
            nOpenSocket?.close()
        } catch (ex: IOException) {
            //
        }
    }

    private fun imprInfDatamaxApex2() {
        val t: Thread = object : Thread() {
            override fun run() {
                fAnchoPapel = 48
                val docExPCLLP = DocumentExPCL_LP(5)
                cabeceraInfDatamaxApex2(docExPCLLP)
                lineasInfDatamaxApex2(docExPCLLP)
                pieInfDatamaxApex2(docExPCLLP)
                val printData: ByteArray = docExPCLLP.documentData
                var conn: ConnectionBase? = null
                try {
                    conn = Connection_Bluetooth.createClient(prefs.getString("impresoraBT", ""))
                    if (!conn.getIsOpen()) {
                        conn.open()
                    }
                    conn.write(printData)
                    sleep(2000)
                    // Signals to close connection
                    conn.close()
                    // Desactivamos el mensaje
                    mHandler.sendEmptyMessage(0)
                    destruir()
                } catch (e: Exception) {
                    //signals to close connection
                    conn?.close()
                    e.printStackTrace()
                }
            }
        }
        t.start()
    }

    private fun imprimirInforme() {
        val t: Thread = object : Thread() {
            override fun run() {
                Looper.prepare()
                var texto = ""
                try {
                    val os = mBluetoothSocket.outputStream
                    texto += cabeceraInforme()
                    os.write(stringABytes(texto))
                    // Pausa.
                    SystemClock.sleep(500)
                    lineasInforme(os)
                    texto = ""
                    // Nueva pausa.
                    SystemClock.sleep(500)
                    texto += pieInforme()
                    os.write(stringABytes(texto))

                    // Llamo a destruir porque he comprobado que la clase no pasa por el método onDestroy() (supongo que porque
                    // no hereda de Activity), así me aseguro de cerrar el socket y los demás objetos abiertos.
                    destruir()
                } catch (e: Exception) {
                    Toast.makeText(fContexto, e.message, Toast.LENGTH_LONG).show()
                }
                Looper.loop()
            }
        }
        t.start()
    }

    private fun lineasInfDatamaxApex2(docExPCL_LP: DocumentExPCL_LP) {
        var queDescr: String

        val stockDao: StockDao? = MyDatabase.getInstance(fContexto)?.stockDao()
        val lDatosInf = stockDao?.getInfStock() ?: emptyList<DatosInfStock>().toMutableList()

        for (dato in lDatosInf) {
            queDescr = dato.descripcion
            queDescr = if (queDescr.length > 25) queDescr.substring(0, 25) else queDescr +
                    String(CharArray(25 - queDescr.length)).replace("\u0000", " ")
            docExPCL_LP.writeTextPartial(queDescr)
            var sTotalEntradas: String
            sTotalEntradas = if (dato.ent != "") dato.ent.replace(',', '.') else "0.0"
            val dTotalEntradas = sTotalEntradas.toDouble()
            sTotalEntradas = String.format(fFtoDecCant, dTotalEntradas)
            if (sTotalEntradas.length < 7)
                sTotalEntradas = String(CharArray(7 - sTotalEntradas.length)).replace("\u0000", " ") + sTotalEntradas
            docExPCL_LP.writeTextPartial(sTotalEntradas)
            var sTotalSalidas: String
            sTotalSalidas = if (dato.sal != "") dato.sal.replace(',', '.') else "0.0"
            val dTotalSalidas = sTotalSalidas.toDouble()
            sTotalSalidas = String.format(fFtoDecCant, dTotalSalidas)
            if (sTotalSalidas.length < 7)
                sTotalSalidas = String(CharArray(7 - sTotalSalidas.length)).replace("\u0000", " ") + sTotalSalidas
            docExPCL_LP.writeTextPartial(sTotalSalidas)
            val dDiferencia = dTotalEntradas - dTotalSalidas
            var sDiferencia = String.format(fFtoDecCant, dDiferencia)
            if (sDiferencia.length < 9)
                sDiferencia = String(CharArray(9 - sDiferencia.length)).replace("\u0000", " ") + sDiferencia
            docExPCL_LP.writeTextPartial(sDiferencia)
            docExPCL_LP.writeText("")
            fElementosImpresos++
        }
    }

    private fun lineasInforme(os: OutputStream) {
        var result = ""
        var queCodigo: String
        var queDescr: String
        var queEmpresa: String

        val stockDao: StockDao? = MyDatabase.getInstance(fContexto)?.stockDao()
        val lDatosInf = stockDao?.getInfStock() ?: emptyList<DatosInfStock>().toMutableList()

        for (dato in lDatosInf) {
            queCodigo = dato.codigo
            queDescr = dato.descripcion
            queEmpresa = ponerCeros(dato.empresa.toString(), ancho_empresa)
            if (queCodigo.length > 8) result += queCodigo.substring(0, 8)
            else {
                queCodigo += String(CharArray(8 - queCodigo.length)).replace("\u0000", " ")
                result += queCodigo
            }
            result = "$result "
            if (queDescr.length > 21) result += queDescr.substring(0, 21)
            else {
                queDescr += String(CharArray(21 - queDescr.length)).replace("\u0000", " ")
                result += queDescr
            }
            result = "$result $queEmpresa"
            var sTotalEntradas: String
            sTotalEntradas = if (dato.ent != "") dato.ent.replace(',', '.') else "0.0"
            val dTotalEntradas = sTotalEntradas.toDouble()
            sTotalEntradas = String.format(fFtoDecCant, dTotalEntradas)
            if (sTotalEntradas.length < 7)
                sTotalEntradas = String(CharArray(7 - sTotalEntradas.length)).replace("\u0000", " ") + sTotalEntradas
            result += sTotalEntradas
            result = "$result  "
            var sTotalSalidas: String
            sTotalSalidas = if (dato.sal != "") dato.sal.replace(',', '.') else "0.0"
            val dTotalSalidas = sTotalSalidas.toDouble()
            sTotalSalidas = String.format(fFtoDecCant, dTotalSalidas)
            if (sTotalSalidas.length < 7)
                sTotalSalidas = String(CharArray(7 - sTotalSalidas.length)).replace("\u0000", " ") + sTotalSalidas
            result += sTotalSalidas
            result = "$result  "
            val dDiferencia = dTotalEntradas - dTotalSalidas
            var sDiferencia = String.format(fFtoDecCant, dDiferencia)
            if (sDiferencia.length < 7)
                sDiferencia = String(CharArray(7 - sDiferencia.length)).replace("\u0000", " ") + sDiferencia
            result += sDiferencia
            result += CCSALTOLINEA
            fElementosImpresos++
            try {
                os.write(stringABytes(result))
                // Nueva pausa.
                SystemClock.sleep(500)
                result = ""
            } catch (e: Exception) {
                Toast.makeText(fContexto, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cabeceraInfDatamaxApex2(docExPCL_LP: DocumentExPCL_LP) {
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var fFechaHora = df.format(tim)
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        fFechaHora = dfHora.format(tim) + "  " + fFechaHora
        fFechaHora =
            String(CharArray(fAnchoPapel - fFechaHora.length)).replace("\u0000", " ") + fFechaHora
        docExPCL_LP.writeText(fFechaHora)
        docExPCL_LP.writeText("Listado de Stock")
        var lineaSimple = ""
        for (x in 0 until fAnchoPapel) {
            lineaSimple = "$lineaSimple-"
        }
        docExPCL_LP.writeText(lineaSimple)
        docExPCL_LP.writeText("DESCRIPCION               CARGA VENDIDO   DIFER.")
        docExPCL_LP.writeText(lineaSimple)
    }

    private fun cabeceraInforme(): String {
        var lineaSimple: String
        var result = ""
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var fFechaHora = df.format(tim)
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        fFechaHora = dfHora.format(tim) + "  " + fFechaHora
        fFechaHora = String(CharArray(60 - fFechaHora.length)).replace("\u0000", " ") + fFechaHora
        result = result + fFechaHora + CCSALTOLINEA
        result = "$result                    Listado de Stock$CCSALTOLINEA"
        lineaSimple = ""
        for (x in 0..59) {
            lineaSimple = "$lineaSimple-"
        }
        result = result + lineaSimple + CCSALTOLINEA
        result =
            result + "Codigo   Descripcion       Empresa  Carga  Vendido    Difer." + CCSALTOLINEA
        result = result + lineaSimple + CCSALTOLINEA
        return result
    }

    private fun pieInfDatamaxApex2(docExPCL_LP: DocumentExPCL_LP) {
        var lineaSimple = ""
        for (x in 0 until fAnchoPapel) {
            lineaSimple = "$lineaSimple-"
        }
        docExPCL_LP.writeText(lineaSimple)
        docExPCL_LP.writeText("Elementos impresos: $fElementosImpresos")
        docExPCL_LP.writeText(lineaSimple)
        // Imprimimos siete líneas en blanco para hacer avanzar el papel.
        for (x in 1..7) {
            docExPCL_LP.writeText("")
        }
    }

    private fun pieInforme(): String {
        var lineaSimple = ""
        var result = ""
        for (x in 0..59) {
            lineaSimple = "$lineaSimple-"
        }
        result = result + lineaSimple + CCSALTOLINEA
        result = "$result      Elementos impresos:     $fElementosImpresos$CCSALTOLINEA"
        result = result + lineaSimple + CCSALTOLINEA
        return result
    }



}
