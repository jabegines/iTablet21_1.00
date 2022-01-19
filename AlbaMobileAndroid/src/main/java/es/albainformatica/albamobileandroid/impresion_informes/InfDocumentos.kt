package es.albainformatica.albamobileandroid.impresion_informes

import es.albainformatica.albamobileandroid.cobros.CobrosClase
import android.content.SharedPreferences
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.preference.PreferenceManager
import android.widget.Toast
import android.content.Intent
import datamaxoneil.printer.DocumentExPCL_LP
import datamaxoneil.connection.ConnectionBase
import datamaxoneil.connection.Connection_Bluetooth
import android.os.Looper
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.CobrosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 11/05/2016.
 */
class InfDocumentos(contexto: Context): Runnable {
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private var fCobros: CobrosClase = CobrosClase(contexto)
    private var fContexto: Context = contexto
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(fContexto)

    private var fFtoDecImpIva: String = fConfiguracion.formatoDecImptesIva()
    private var fElementosImpresos = 0
    private var fTotalVentas = 0.0
    private var fTotalCobrado = 0.0
    private var fDesdeFecha: String = ""
    private var fHastaFecha: String = ""
    private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mBluetoothDevice: BluetoothDevice
    private var fAnchoPapel: Short = 0
    private var fImpresora = fConfiguracion.impresora()

    private val ccSaltoLinea = "\n"



    private fun destruir() {
        try {
            mBluetoothSocket.close()
        } catch (e: Exception) {
            //
        }
    }

    fun imprimir(desdeFecha: String, hastaFecha: String) {
        fDesdeFecha = desdeFecha
        fHastaFecha = hastaFecha

        // Intentamos conectar con Bluetooth. Para ello pasamos la dirección de la impresora.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(fContexto, "No se pudo conectar con Bluetooth", Toast.LENGTH_SHORT).show()
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                fContexto.startActivity(enableBtIntent)
            } else {
                // Leemos la dirección de la impresora Bluetooth de las preferencias.
                val mDeviceAddress = prefs.getString("impresoraBT", "")
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
            if (fImpresora == IMPRESORA_DATAMAX_APEX_2) {
                imprInfDatamaxApex2()
            } else {
                // Obtenemos un bluetoothsocket y lo conectamos. A partir de entonces, llamamos a imprimirDoc().
                mBluetoothSocket =
                    mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
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

    private fun closeSocket(nOpenSocket: BluetoothSocket) {
        try {
            nOpenSocket.close()
        } catch (ex: IOException) {
            //
        }
    }

    private fun imprInfDatamaxApex2() {
        val t: Thread = object : Thread() {
            override fun run() {
                fAnchoPapel = 48
                val docExPCLLP = DocumentExPCL_LP(5)
                if (hayDocumentos()) {
                    cabeceraInfDatamaxApex2(docExPCLLP)
                    lineasInfDatamaxApex2(docExPCLLP)
                    pieInfDatamaxApex2(docExPCLLP)
                }
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

    // Usaremos la funcion stringABytes, que está en Miscelan, para poder imprimir los caracteres especiales que,
    // de otra manera, nos darían problemas de mala impresión y desplazamiento de las columnas. Por ahora controlamos
    // los caracteres para la impresora Star DP8340. Hay que configurarla con los switches 6, 7 y 8 en Off (hacia abajo,
    // por defecto en esta impresora vienen hacia arriba).
    private fun imprimirInforme() {
        val t: Thread = object : Thread() {
            override fun run() {
                Looper.prepare()
                var texto = ""
                try {
                    val os = mBluetoothSocket.outputStream
                    if (hayDocumentos()) {
                        texto += cabeceraInforme()
                        os.write(stringABytes(texto))
                        texto = ""
                        // Pausa.
                        SystemClock.sleep(500)
                        fElementosImpresos = 0
                        texto += lineasInforme()
                        os.write(stringABytes(texto))
                        texto = ""

                        // Nueva pausa.
                        SystemClock.sleep(500)
                        texto += pieInforme()
                        os.write(stringABytes(texto))
                        texto = ""
                        SystemClock.sleep(500)

                        // Vemos los cobros
                        texto += verCobros()
                        os.write(stringABytes(texto))
                        texto = ""
                    }
                    if (fTotalVentas != 0.0 || fTotalCobrado != 0.0) {
                        texto += imprimeTotales()
                        os.write(stringABytes(texto))
                    }

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

    @SuppressLint("Range")
    private fun lineasInfDatamaxApex2(docExPCL_LP: DocumentExPCL_LP) {
        var queTipoDoc: String
        var queSerieNum: String
        var queNombre: String

        val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(fContexto)?.cabecerasDao()
        val lDocumentos = cabecerasDao?.getInfDocumentos(fechaEnJulian(fDesdeFecha), fechaEnJulian(fHastaFecha))
                        ?: emptyList<DatosVerDocs>().toMutableList()

        for (doc in lDocumentos) {
            val fCobrado = fCobros.dimeCobrosDoc(doc.tipoDoc.toString(), doc.almacen.toString(), doc.serie,
                                doc.numero.toString(), doc.ejercicio.toString(), doc.empresa.toString())

            queTipoDoc = doc.tipoDoc.toString()
            when (queTipoDoc) {
                "1" -> queTipoDoc = "F"
                "2" -> queTipoDoc = "A"
                "3" -> queTipoDoc = "P"
                "6" -> queTipoDoc = "R"
            }
            docExPCL_LP.writeTextPartial(queTipoDoc)
            queSerieNum = doc.serie.trim { it <= ' ' } + "/" + doc.numero.toString().trim { it <= ' ' }
            if (queSerieNum.length < 8) queSerieNum += String(CharArray(8 - queSerieNum.length)).replace("\u0000", " ")
            docExPCL_LP.writeTextPartial("$queSerieNum ")
            queNombre = if (fConfiguracion.aconsNomComercial()) doc.nombreComercial
            else doc. nombre
            queNombre = if (queNombre.length > 22) queNombre.substring(0, 22)
                        else queNombre + String(CharArray(22 - queNombre.length)).replace("\u0000", " ")
            docExPCL_LP.writeTextPartial("$queNombre ")
            var sTotal = doc.total.replace(',', '.')
            val dTotal = sTotal.toDouble()
            sTotal = String.format(fFtoDecImpIva, dTotal)
            if (sTotal.length < 7) sTotal = String(CharArray(7 - sTotal.length)).replace("\u0000", " ") + sTotal
            docExPCL_LP.writeTextPartial("$sTotal ")
            var sCobrado = String.format(fFtoDecImpIva, fCobrado)
            if (sCobrado.length < 7) sCobrado = String(CharArray(7 - sCobrado.length)).replace("\u0000", " ") + sCobrado
            docExPCL_LP.writeTextPartial(sCobrado)
            docExPCL_LP.writeText("")
            fElementosImpresos++
        }
    }

    @SuppressLint("Range")
    private fun verCobros(): String {
        val result =
            StringBuilder("COBROS REALIZADOS EN LA TABLET:$ccSaltoLinea$ccSaltoLinea")
        var queTipoDoc: String
        var queSerieNum: String
        var queNombre: String
        var fTotalCobros = 0.0
        var sQuery = "SELECT A.tipodoc, A.serie, A.numero, A.cobro"
        sQuery =
            if (fConfiguracion.aconsNomComercial()) "$sQuery, B.nomco nombre" else "$sQuery, B.nomfi nombre"
        sQuery = sQuery + " FROM cobros A" +
                " LEFT JOIN clientes B ON B.cliente = A.cliente" +
                " LEFT JOIN cabeceras C ON C.tipodoc = A.tipodoc AND C.alm = A.alm AND C.serie = A.serie AND C.numero = A.numero AND C.ejer = A.ejer" +
                " WHERE (A.vapunte > 0 OR (A.estado = 'N' AND C.estado = 'X'))"
        sQuery =
            "$sQuery AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' || substr(A.fechacobro, 1, 2)) >= julianday('" + fechaEnJulian(
                fDesdeFecha
            ) + "'))"
        sQuery =
            "$sQuery AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' || substr(A.fechacobro, 1, 2)) <= julianday('" + fechaEnJulian(
                fHastaFecha
            ) + "'))"

        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContexto)?.cobrosDao()
        val lCobros = cobrosDao?.getCobrosPorFechas(fechaEnJulian(fDesdeFecha), fechaEnJulian(fHastaFecha))
                            ?: emptyList<DatosInfCobros>().toMutableList()

        for (cobro in lCobros) {
            queTipoDoc = cobro.tipoDoc.toString()
            when (queTipoDoc) {
                "1" -> queTipoDoc = "F"
                "2" -> queTipoDoc = "A"
                "3" -> queTipoDoc = "P"
                "6" -> queTipoDoc = "R"
            }
            result.append(queTipoDoc).append(" ")
            queSerieNum = cobro.serie.trim { it <= ' ' } + "/" + cobro.numero.trim { it <= ' ' }
            if (queSerieNum.length < 10) queSerieNum += String(CharArray(10 - queSerieNum.length)).replace("\u0000", " ")
            result.append(queSerieNum).append(" ")
            queNombre = if (fConfiguracion.aconsNomComercial()) cobro.nombreComercial
            else cobro.nombre
            if (queNombre.length > 30) result.append(queNombre.substring(0, 30))
            else { queNombre += String(CharArray(30 - queNombre.length)).replace("\u0000", " ")
                result.append(queNombre)
            }
            result.append("  ")
            var sCobrado = cobro.cobro.replace(',', '.')
            val dCobrado = sCobrado.toDouble()
            fTotalCobros += dCobrado
            sCobrado = String.format(fFtoDecImpIva, dCobrado)
            if (sCobrado.length < 9) sCobrado = String(CharArray(9 - sCobrado.length)).replace("\u0000", " ") + sCobrado
            result.append(sCobrado)
            result.append(ccSaltoLinea)

            // Pausa.
            SystemClock.sleep(500)
        }

        val lineaSimple = StringBuilder()
        for (x in 0..59) {
            lineaSimple.append("-")
        }
        var sTotalCobros = String.format(fFtoDecImpIva, fTotalCobros)
        if (sTotalCobros.length > 7) sTotalCobros = "*******"
        sTotalCobros = StringOfChar(" ", 7 - sTotalCobros.length) + sTotalCobros
        result.append(lineaSimple).append(ccSaltoLinea)
        result.append("TOTAL COBROS:").append(StringOfChar(" ", 34)).append(sTotalCobros).append(ccSaltoLinea)
        result.append(lineaSimple).append(StringOfChar(ccSaltoLinea, 3))

        return result.toString()
    }

    @SuppressLint("Range")
    private fun lineasInforme(): String {
        var queTipoDoc: String
        var queSerieNum: String
        var queNombre: String
        val result = StringBuilder()

        try {
            val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(fContexto)?.cabecerasDao()
            val lDocumentos = cabecerasDao?.getInfDocumentos(fechaEnJulian(fDesdeFecha), fechaEnJulian(fHastaFecha))
                ?: emptyList<DatosVerDocs>().toMutableList()

            for (doc in lDocumentos) {
                @SuppressLint("Range") val fCobrado = fCobros.dimeCobrosDoc(doc.tipoDoc.toString(),
                    doc.almacen.toString(), doc.serie, doc.numero.toString(), doc.ejercicio.toString(),
                    doc.empresa.toString())

                queTipoDoc = doc.tipoDoc.toString()
                when (queTipoDoc) {
                    "1" -> queTipoDoc = "F"
                    "2" -> queTipoDoc = "A"
                    "3" -> queTipoDoc = "P"
                    "6" -> queTipoDoc = "R"
                }
                result.append(queTipoDoc).append(" ")
                queSerieNum = doc.serie.trim { it <= ' ' } + "/" + doc.numero.toString().trim { it <= ' ' }
                if (queSerieNum.length < 10) queSerieNum += String(CharArray(10 - queSerieNum.length)).replace("\u0000", " ")
                result.append(queSerieNum).append(" ")
                queNombre = if (fConfiguracion.aconsNomComercial()) doc.nombreComercial
                else doc.nombre
                if (queNombre.length > 30) result.append(queNombre.substring(0, 30))
                else {
                    queNombre += String(CharArray(30 - queNombre.length)).replace("\u0000", " ")
                    result.append(queNombre)
                }
                result.append("  ")
                var sTotal = doc.total.replace(',', '.')
                val dTotal = sTotal.toDouble()
                sTotal = String.format(fFtoDecImpIva, dTotal)
                if (sTotal.length < 7) sTotal =
                    String(CharArray(7 - sTotal.length)).replace("\u0000", " ") + sTotal
                result.append(sTotal).append(" ")
                var sCobrado = String.format(fFtoDecImpIva, fCobrado)
                if (sCobrado.length < 7) sCobrado =
                    String(CharArray(7 - sCobrado.length)).replace("\u0000", " ") + sCobrado
                result.append(sCobrado)
                result.append(ccSaltoLinea)
                fElementosImpresos++
                fTotalVentas += dTotal
                fTotalCobrado += fCobrado

                // Pausa.
                SystemClock.sleep(500)
            }

        } catch (e: Exception) {
            Toast.makeText(fContexto, e.message, Toast.LENGTH_LONG).show()
        }
        return result.toString()
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
        docExPCL_LP.writeText("Documentos vendidos ")
        val lineaSimple = StringBuilder()
        for (x in 0 until fAnchoPapel) {
            lineaSimple.append("-")
        }
        docExPCL_LP.writeText(lineaSimple.toString())
        docExPCL_LP.writeText("NUM.DOC   CLIENTE                IMPORTE   COBRO")
        docExPCL_LP.writeText(lineaSimple.toString())
    }

    private fun cabeceraInforme(): String {
        var result = ""
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var fFechaHora = df.format(tim)
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        fFechaHora = dfHora.format(tim) + "  " + fFechaHora
        fFechaHora = String(CharArray(60 - fFechaHora.length)).replace("\u0000", " ") + fFechaHora
        result = result + fFechaHora + ccSaltoLinea
        result = "$result               Documentos vendidos $ccSaltoLinea"
        val lineaSimple = StringBuilder()
        for (x in 0..59) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        result =
            result + "Numero Doc   Cliente                         Importe Cobrado" + ccSaltoLinea
        result = result + lineaSimple + ccSaltoLinea
        return result
    }

    private fun pieInfDatamaxApex2(docExPCL_LP: DocumentExPCL_LP) {
        val lineaSimple = StringBuilder()
        for (x in 0 until fAnchoPapel) {
            lineaSimple.append("-")
        }
        docExPCL_LP.writeText(lineaSimple.toString())
        docExPCL_LP.writeText("Elementos impresos: $fElementosImpresos")
        docExPCL_LP.writeText(lineaSimple.toString())
        // Imprimimos siete líneas en blanco para hacer avanzar el papel.
        for (x in 1..7) {
            docExPCL_LP.writeText("")
        }
    }

    private fun pieInforme(): String {
        val lineaSimple = StringBuilder()
        var result = ""
        for (x in 0..59) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        val sElemImpresos = "Elementos impresos: $fElementosImpresos"
        result += sElemImpresos
        var sTotalVentas: String = String.format(fFtoDecImpIva, fTotalVentas)
        var sTotalCobrado: String = String.format(fFtoDecImpIva, fTotalCobrado)
        sTotalVentas = if (sTotalVentas.length > 7) "*******" else StringOfChar(
            " ",
            7 - sTotalVentas.length
        ) + sTotalVentas
        sTotalCobrado = if (sTotalCobrado.length > 7) "*******" else StringOfChar(
            " ",
            7 - sTotalCobrado.length
        ) + sTotalCobrado
        result = result + StringOfChar(" ", 7) + "SUBTOTALES: " + StringOfChar(
            " ",
            5
        ) + sTotalVentas + StringOfChar(" ", 1) + sTotalCobrado + ccSaltoLinea
        result = result + lineaSimple + StringOfChar(ccSaltoLinea, 2)
        return result
    }

    private fun imprimeTotales(): String {
        val lineaSimple = StringBuilder()
        var result = ""
        for (x in 0..59) {
            lineaSimple.append("-")
        }
        result = result + lineaSimple + ccSaltoLinea
        val sTotalVentas = String.format(fFtoDecImpIva, fTotalVentas)
        val sTotalCobrado = String.format(fFtoDecImpIva, fTotalCobrado)
        result =
            result + StringOfChar(" ", 28) + "TOTALES: " + StringOfChar(" ", 9) + sTotalVentas +
                    StringOfChar(" ", 2) + sTotalCobrado + ccSaltoLinea
        result = result + lineaSimple + StringOfChar(ccSaltoLinea, 8)
        return result
    }

    private fun hayDocumentos(): Boolean {
        val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(fContexto)?.cabecerasDao()
        val lDocumentos = cabecerasDao?.getInfDocumentos(fechaEnJulian(fDesdeFecha), fechaEnJulian(fHastaFecha))
            ?: emptyList<DatosVerDocs>().toMutableList()

        return (lDocumentos.count() > 0)
    }

}