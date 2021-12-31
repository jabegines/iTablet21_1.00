 package es.albainformatica.albamobileandroid.impresion_informes

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.preference.PreferenceManager
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.dao.DocsCabPiesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.ventas.Documento
import java.io.IOException
import java.io.OutputStream
import java.util.*


 class ImprZebra(context: Context): Runnable {
     private val fContexto = context
     private var fDocumento: Documento = Comunicador.fDocumento
     private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
     private var fFormasPago: FormasPagoClase = FormasPagoClase(context)
     private var fPendiente: PendienteClase = PendienteClase(context)
     private val docsCabPiesDao: DocsCabPiesDao? = MyDatabase.getInstance(context)?.docsCabPiesDao()
     private lateinit var prefs: SharedPreferences
     private var db: BaseDatos = BaseDatos(context)
     private var dbAlba: SQLiteDatabase = db.readableDatabase

     private lateinit var fFtoCant: String
     private lateinit var fFtoPrBase: String
     private lateinit var fFtoPrII: String
     private lateinit var fFtoImpBase: String
     private lateinit var fFtoImpII: String
     private var fVtaIvaIncluido = true

     private val fCR = 13.toChar()
     private val fLF = 10.toChar()
     //private val fDOBLEANCHO = "! U1 SETLP 7 1 48"
     //private val fNORMAL = "! U1 SETLP 7 0 24"

     private val applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
     private var mBluetoothConnectProgressDialog: ProgressDialog? = null
     private lateinit var mBluetoothAdapter: BluetoothAdapter
     private lateinit var mBluetoothSocket: BluetoothSocket
     private lateinit var mBluetoothDevice: BluetoothDevice

     var fTerminado = false
     var fImprimiendo = false
     private var queImprimir: Short = 1

     private val fImprimirDocumento: Short = 1
     //private val fImprimirCarga: Short = 2

     private var fImpresora = IMPRESORA_ZEBRA_80
     private var anchoPapel: Short = 48


     init {
         fImpresora = fConfiguracion.impresora()
         inicializarControles()
     }


     private fun destruir() {
         dbAlba.close()
         db.close()

         try {
             mBluetoothSocket.close()
         } catch (e: Exception) {
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
             val mDeviceAddress: String = prefs.getString("impresoraBT", "") ?: ""
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


     override fun run() {
         try {
             // Obtenemos un bluetoothsocket y lo conectamos. A partir de entonces, llamamos a imprimirDoc().
             mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
             mBluetoothAdapter.cancelDiscovery()
             mBluetoothSocket.connect()
             mHandler.sendEmptyMessage(0)
             fImprimiendo = true
             //if (queImprimir == fImprimirDocumento) imprimirDoc()
             if (queImprimir == fImprimirDocumento) imprimirDocZPL()
             //else if (queImprimir == fImprimirCarga) imprimeCarga()
         } catch (eConnectException: IOException) {
             closeSocket(mBluetoothSocket)
         }
     }


     private val mHandler: Handler = object : Handler() {
         override fun handleMessage(msg: Message) {
             mBluetoothConnectProgressDialog?.dismiss()
         }
     }


     private fun closeSocket(nOpenSocket: BluetoothSocket) {
         try {
             nOpenSocket.close()
         } catch (ex: IOException) {
         }
     }


     private fun inicializarControles() {
         fFtoCant = fConfiguracion.formatoDecCantidad()
         fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
         fFtoPrII = fConfiguracion.formatoDecPrecioIva()
         fFtoImpBase = fConfiguracion.formatoDecImptesBase()
         fFtoImpII = fConfiguracion.formatoDecImptesIva()
         fVtaIvaIncluido = fConfiguracion.ivaIncluido(fDocumento.fEmpresa.toInt())

         // Leemos las preferencias de la aplicación;
         prefs = PreferenceManager.getDefaultSharedPreferences(fContexto)

         if (fImpresora == IMPRESORA_ZEBRA_80) anchoPapel = 64
     }


     private fun imprimirDocZPL() {
         val t: Thread = object: Thread() {
             override fun run() {
                 var texto: String
                 try {
                     val os = mBluetoothSocket.outputStream
                     texto = imprCabeceraZPL()

                     texto += imprDatClteDoc80ZPL()
                     os.write(texto.toByteArray())
                     // Pausa.
                     SystemClock.sleep(500)

                     texto = imprCabLin80ZPL()
                     os.write(texto.toByteArray())

                     imprLineasPosit80ZPL(os)
                     imprLineasNeg80ZPL(os)

                     imprCabPie80ZPL(os)
                     imprBases80ZPL(os)
                     texto = ""
                     // Nueva pausa.
                     SystemClock.sleep(500)
                     if (fDocumento.fTipoDoc == TIPODOC_FACTURA) {
                         texto += imprFPagoZPL()
                         SystemClock.sleep(500)
                     }
                     texto += imprPieZPL()
                     os.write(texto.toByteArray())

                     // Llamo a destruir porque he comprobado que la clase no pasa por el método onDestroy() (supongo que porque
                     // no hereda de Activity), así me aseguro de cerrar el socket y los demás objetos abiertos.
                     destruir()
                     fTerminado = true
                     fImprimiendo = false

                 } catch (e: java.lang.Exception) {
                     fTerminado = true
                 }
             }
         }
         t.start()
     }


     private fun imprCabeceraZPL(): String {
         val cabeceraDoc = docsCabPiesDao?.cabeceraDoc(fDocumento.fEmpresa) ?: ""
         val result = StringBuilder()

         val lineasDobles = StringBuilder()
         for (x in 0 until anchoPapel) {
             lineasDobles.append("=")
         }
         val lineasCabDoc = fConfiguracion.lineasCabDocZebra(fDocumento.fEmpresa, lineasDobles.toString())

         // Asignamos la letra K al la fuente Swiss, para poder usar caracteres especiales
         result.append("^XA^CWK,E: TT0003M_^XZ").append(fCR).append(fLF)
         // Asignamos el tipo de media como continuo y la orientación inversa. Longitud del papel 240
         result.append("^XA^MNN^POI^LL240").append(fCR).append(fLF)
         // Cambiamos la codificación a UTF-8 para que se puedan imprimir caracteres especiales
         result.append("^CI28").append(fCR).append(fLF)
         // Cambiamos al font K, altura de 12
         result.append("^CFK,12").append(fCR).append(fLF)
         // Posición 0,0 y empezamos a imprimir
         result.append("^FT0,0").append(fCR).append(fLF)
         result.append("^FD$lineasDobles^FS").append(fCR).append(fLF)
         // Posición 0,70 y altura de 30
         result.append("^FT0,45^AKN,30").append(fCR).append(fLF)
         result.append("^FD$cabeceraDoc^FS").append(fCR).append(fLF)

         result.append(lineasCabDoc)
         result.append("^XZ")

         return result.toString()
     }

     private fun imprDatClteDoc80(): String {
         var result: String
         val sLongDatosClte = anchoPapel
         result = ajustarCadena(fDocumento.fClientes.getCodigo() + " " + fDocumento.fClientes.getNFiscal(), sLongDatosClte.toInt(), true) + fCR + fLF
         result = result + ajustarCadena(fDocumento.fClientes.getNComercial(), sLongDatosClte.toInt(), true) + fCR + fLF
         result = result + ajustarCadena(fDocumento.fClientes.getDireccion(), sLongDatosClte.toInt(), true) + fCR + fLF
         result = result + ajustarCadena(fDocumento.fClientes.getCodPostal() + " " + fDocumento.fClientes.getPoblacion(), sLongDatosClte.toInt(), true) + fCR + fLF
         result = result + ajustarCadena(fDocumento.fClientes.getProvincia(), sLongDatosClte.toInt(), true) + fCR + fLF
         result = result + ajustarCadena("C.I.F.: " + fDocumento.fClientes.getCIF(), sLongDatosClte.toInt(), true) + fCR + fLF
         result = result + fCR + fLF
         result = result + "Vendedor: " + fConfiguracion.vendedor() + " " + fConfiguracion.nombreVendedor() + fCR + fLF
         result = result + "Fecha: " + fDocumento.fFecha + "     Hora: " + fDocumento.fHora + fCR + fLF
         result = result + "Documento: " + tipoDocAsString(fDocumento.fTipoDoc)
         result = result + "     Numero: " + fDocumento.serie + "/" + fDocumento.numero
         result = result + fCR + fLF + fCR + fLF + fCR + fLF
         return result
     }

     private fun imprDatClteDoc80ZPL(): String {
         val sLongDatosClte = anchoPapel

         var result = "^XA^LL290"
         // Posición 0,0 y altura de 20
         result += "^FT0,0^AKN,25$fCR$fLF"
         result += "^FD" + ajustarCadena(fDocumento.fClientes.getCodigo() + " " +
                 fDocumento.fClientes.getNFiscal(), sLongDatosClte.toInt(), true) + fCR + fLF + "^FS" + fCR + fLF

         result += "^FT0,50^AKN,20$fCR$fLF"
         result += "^FD" + ajustarCadena(fDocumento.fClientes.getNComercial(), sLongDatosClte.toInt(), true) + "^FS" + fCR + fLF

         result += "^FT0,75^AKN,20$fCR$fLF"
         result += "^FD" + ajustarCadena(fDocumento.fClientes.getDireccion(), sLongDatosClte.toInt(), true) + "^FS" + fCR + fLF

         result += "^FT0,100^AKN,20$fCR$fLF"
         result += "^FD" + ajustarCadena(fDocumento.fClientes.getCodPostal() + " " +
                 fDocumento.fClientes.getPoblacion(), sLongDatosClte.toInt(), true) + "^FS" + fCR + fLF

         result += "^FT0,125^AKN,20$fCR$fLF"
         result += "^FD" + ajustarCadena(fDocumento.fClientes.getProvincia(), sLongDatosClte.toInt(), true) + "^FS" + fCR + fLF

         result += "^FT0,150^AKN,20$fCR$fLF"
         result += "^FD" + ajustarCadena("C.I.F.: " + fDocumento.fClientes.getCIF(), sLongDatosClte.toInt(), true) + "^FS" + fCR + fLF + fCR + fLF

         result += "^FT0,190^AKN,20$fCR$fLF"
         result += "^FD" + "Vendedor: " + fConfiguracion.vendedor() + " " + fConfiguracion.nombreVendedor() + "^FS" + fCR + fLF

         result += "^FT0,220^AKN,20$fCR$fLF"
         result += "^FD" + "Fecha: " + fDocumento.fFecha + "^FS" + fCR + fLF
         result += "^FT250,220^AKN,20$fCR$fLF"
         result += "^FD" + "Hora: " + fDocumento.fHora + "^FS" + fCR + fLF

         result += "^FT0,250^AKN,20$fCR$fLF"
         result = result + "^FDDocumento: " + tipoDocAsString(fDocumento.fTipoDoc) + "^FS"

         result += "^FT250,250^AKN,20$fCR$fLF"
         result += "^FDNumero: " + fDocumento.serie + "/" + fDocumento.numero + "^FS"

         result = "$result^XZ"
         return result
     }

     private fun imprCabLin80(): String {
         val lineaSimple: java.lang.StringBuilder = java.lang.StringBuilder()
         var result: String = "COD." + StringOfChar(" ", 4) + "ARTICULO" + StringOfChar(" ", 12) +
                 "UNID" + StringOfChar(" ", 2) + "PRECIO" + StringOfChar(" ", 3) +
                 "TOTAL" + fCR + fLF
         for (x in 0 until anchoPapel) {
             lineaSimple.append("-")
         }
         result = result + lineaSimple + fCR + fLF
         return result
     }

     private fun imprCabLin80ZPL(): String {
         val lineaSimple: java.lang.StringBuilder = java.lang.StringBuilder()
         for (x in 0 until anchoPapel) {
             lineaSimple.append("-")
         }
         val result = java.lang.StringBuilder()

         result.append("^XA^LL40")
         result.append("^FT0,0^AKN,20").append(fCR).append(fLF)
         result.append("^FDCOD.^FS").append(fCR).append(fLF)
         result.append("^FT90,0^AKN,20").append(fCR).append(fLF)
         result.append("^FDARTICULO^FS").append(fCR).append(fLF)
         result.append("^FT330,0^AKN,20").append(fCR).append(fLF)
         result.append("^FDUNID.^FS").append(fCR).append(fLF)
         result.append("^FT400,0^AKN,20").append(fCR).append(fLF)
         result.append("^FDPRECIO^FS").append(fCR).append(fLF)
         result.append("^FT510,0^AKN,20").append(fCR).append(fLF)
         result.append("^FDTOTAL^FS").append(fCR).append(fLF)

         result.append("^FT0,35^AKN,30").append(fCR).append(fLF)
         result.append("^FD$lineaSimple^FS").append(fCR).append(fLF)

         result.append("^XZ")
         return result.toString()
     }


     private fun imprLineasPosit80(os: OutputStream) {
         val result = java.lang.StringBuilder()
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

         val tiposIncDao = MyDatabase.getInstance(fContexto)?.tiposIncDao()

         fDocumento.cLineas.moveToFirst()
         while (!fDocumento.cLineas.isAfterLast) {
             sCodigo = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("codigo"))
             sDescr = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("descr"))
             sCajas = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cajas")).replace(",", ".")
             sCant = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cantidad")).replace(",", ".")
             val dCant = sCant.toDouble()
             if (dCant >= 0.0) {
                 if (fVtaIvaIncluido) {
                     sPrecio = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("precioii")).replace(",", ".")
                     sImpte = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("importeii")).replace(",", ".")
                 } else {
                     sPrecio = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("precio")).replace(",", ".")
                     sImpte = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("importe")).replace(",", ".")
                 }
                 result.append(ajustarCadena(sCodigo, lCodigo, true)).append(" ").append(ajustarCadena(sDescr, lDescr, true))
                 val dCajas = sCajas.toDouble()
                 sumaCant += dCant
                 sumaCajas += dCajas
                 sCajas = String.format(fFtoCant, dCajas)
                 sCant = String.format(fFtoCant, dCant)
                 val dPrecio = sPrecio.toDouble()
                 val dImpte = sImpte.toDouble()

                 // Si la línea es sin cargo lo indicamos
                 if (fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("flag")) and FLAGLINEAVENTA_SIN_CARGO > 0) {
                     sPrecio = "SIN"
                     sImpte = "CARGO"
                 } else {
                     sPrecio = String.format(fFtoPrBase, dPrecio)
                     sImpte = String.format(fFtoImpBase, dImpte)
                 }
                 result.append(" ").append(ajustarCadena(sCant, lCant, false)).append(" ")
                     .append(ajustarCadena(sPrecio, lPrecio, false)).append(" ")
                     .append(ajustarCadena(sImpte, lImpte, false))
                 result.append(fCR).append(fLF)

                 // Si la línea tiene cajas las imprimimos
                 if (fDocumento.cLineas.getDouble(fDocumento.cLineas.getColumnIndex("cajas")) != 0.0) {
                     result.append("Cajas: ").append(ajustarCadena(sCajas, lCajas, false)).append(fCR).append(fLF)
                 }
                 // Si la línea tiene descuento lo imprimimos
                 if (fDocumento.cLineas.getDouble(fDocumento.cLineas.getColumnIndex("dto")) != 0.0) {
                     sDto = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("dto"))
                     result.append("% dto.: ").append(ajustarCadena(sDto, 5, false)).append(fCR).append(fLF)
                 }
                 // Si la línea tiene número de lote lo imprimimos.
                 if (fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("lote")) != null
                     && fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("lote")) != ""
                 ) {
                     sLote = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("lote"))
                     result.append("Numero lote: ").append(ajustarCadena(sLote, lLote, true))
                     result.append(fCR).append(fLF)
                 }
                 // Si la línea tiene incidencia la imprimimos
                 if (fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("incidencia")) > 0) {
                     fIncidencia = fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("incidencia"))
                     val queDescrInc = tiposIncDao?.dimeDescripcion(fIncidencia) ?: ""
                     if (queDescrInc != "") {
                         result.append("Incidencia: ").append(fIncidencia).append(" ").append(queDescrInc)
                         result.append(fCR).append(fLF)
                     }
                 }
             }
             fDocumento.cLineas.moveToNext()
         }
         result.append(fCR).append(fLF)
         // Pausa.
         SystemClock.sleep(200)
         sCajas = String.format(fFtoCant, sumaCajas)
         sCant = String.format(fFtoCant, sumaCant)
         result.append("SUMAS: ").append(StringOfChar(" ", 13))
             .append(ajustarCadena(sCajas, lCajas, false)).append(" ")
             .append(ajustarCadena(sCant, lCant, false)).append(fCR).append(fLF)
         val lineaSimple: java.lang.StringBuilder = java.lang.StringBuilder()
         for (x in 0 until anchoPapel) {
             lineaSimple.append("-")
         }
         result.append(lineaSimple).append(fCR).append(fLF)
         try {
             os.write(result.toString().toByteArray())
             // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
             SystemClock.sleep(500)
         } catch (ignored: java.lang.Exception) {
         }
     }


     private fun imprLineasPosit80ZPL(os: OutputStream) {
         val result = java.lang.StringBuilder()
         var sCajas: String
         var sCant: String
         var sumaCajas = 0.0
         var sumaCant = 0.0

         var y = 0
         var primeraLinea = true

         fDocumento.cLineas.moveToFirst()
         while (!fDocumento.cLineas.isAfterLast) {
             sCajas = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cajas")).replace(",", ".")
             sCant = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cantidad")).replace(",", ".")
             val dCant = sCant.toDouble()
             if (dCant >= 0.0) {
                 val dCajas = sCajas.toDouble()
                 sumaCant += dCant
                 sumaCajas += dCajas
                 sCajas = String.format(fFtoCant, dCajas)
                 sCant = String.format(fFtoCant, dCant)

                 imprimirLinea(result, y, sCant)
                 y = imprimirSigLineas(result, y, sCajas)

                 if (primeraLinea) {
                     y += 40
                     primeraLinea = false
                 }
                 else y += 25
             }
             fDocumento.cLineas.moveToNext()
         }
         // Pausa.
         SystemClock.sleep(200)
         if (sumaCajas != 0.0 || sumaCant != 0.0)
             terminaImprLineas(os, result, sumaCajas, sumaCant, y)
     }

     private fun imprLineasNeg80ZPL(os: OutputStream) {
         val result = java.lang.StringBuilder()
         var sCajas: String
         var sCant: String
         var sumaCajas = 0.0
         var sumaCant = 0.0

         var y = 0
         var primeraLinea = true

         fDocumento.cLineas.moveToFirst()
         while (!fDocumento.cLineas.isAfterLast) {
             sCajas = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cajas")).replace(",", ".")
             sCant = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("cantidad")).replace(",", ".")
             val dCant = sCant.toDouble()
             if (dCant < 0.0) {
                 val dCajas = sCajas.toDouble()
                 sumaCant += dCant
                 sumaCajas += dCajas
                 sCajas = String.format(fFtoCant, dCajas)
                 sCant = String.format(fFtoCant, dCant)

                 imprimirLinea(result, y, sCant)
                 y = imprimirSigLineas(result, y, sCajas)

                 if (primeraLinea) {
                     y += 40
                     primeraLinea = false
                 }
                 else y += 25
             }
             fDocumento.cLineas.moveToNext()
         }
         // Pausa.
         SystemClock.sleep(200)
         if (sumaCajas != 0.0 || sumaCant != 0.0)
             terminaImprLineas(os, result, sumaCajas, sumaCant, y)
     }


     private fun terminaImprLineas(os: OutputStream, result: java.lang.StringBuilder, sumaCajas: Double,
                                   sumaCant: Double, y: Int) {
         val sLineas = java.lang.StringBuilder()
         var x = y
         val sCajas = String.format(fFtoCant, sumaCajas)
         val sCant = String.format(fFtoCant, sumaCant)
         val lCajas = 6
         val lCant = 6

         x += 20
         result.append("^FT0,$x^AKN,20")
         result.append("^FD").append("SUMAS:").append("^FS").append(fCR).append(fLF)
         result.append("^FT140,$x^AKN,20")
         result.append("^FD").append(ajustarCadena(sCajas, lCajas, false)).append("^FS").append(fCR).append(fLF)
         result.append("^FT330,$x^AKN,20")
         result.append("^FD").append(ajustarCadena(sCant, lCant, false)).append("^FS").append(fCR).append(fLF)

         x += 20
         val lineaSimple: java.lang.StringBuilder = java.lang.StringBuilder()
         for (z in 0 until anchoPapel) {
             lineaSimple.append("-")
         }
         result.append("^FT0,$x^AKN,30")
         result.append("^FD").append(lineaSimple).append("^FS").append(fCR).append(fLF)

         x += 40
         sLineas.append("^XA^LL$x").append(fCR).append(fLF)
         sLineas.append(result)
         sLineas.append("^XZ")

         try {
             os.write(sLineas.toString().toByteArray())
             // Hacemos una pausa para agilizar los buffers de las impresoras, principalmente de las serie.
             SystemClock.sleep(500)
         } catch (ignored: java.lang.Exception) {
         }
     }

     private fun imprCabPie80ZPL(os: OutputStream) {
         val texto: java.lang.StringBuilder = java.lang.StringBuilder()
         val lineaSimple: java.lang.StringBuilder = java.lang.StringBuilder()
         try {
             texto.append("^XA^LL60").append(fCR).append(fLF)

             for (x in 0 until anchoPapel) {
                 lineaSimple.append("-")
             }
             texto.append("^FT0,0^AKN,30")
             texto.append("^FD$lineaSimple^FS").append(fCR).append(fLF)
             texto.append("^FT10,30^AKN,20")
             texto.append("^FDVENTA^FS").append(fCR).append(fLF)
             texto.append("^FT130,30^AKN,20")
             texto.append("^FDDTO^FS").append(fCR).append(fLF)
             texto.append("^FT220,30^AKN,20").append(fCR).append(fLF)
             texto.append("^FDNETO^FS").append(fCR).append(fLF)
             texto.append("^FT330,30^AKN,20")
             texto.append("^FD%IVA^FS").append(fCR).append(fLF)
             texto.append("^FT450,30^AKN,20")
             texto.append("^FDIVA^FS").append(fCR).append(fLF)
             texto.append("^FT0,50^AKN,30")
             texto.append("^FD$lineaSimple^FS").append(fCR).append(fLF)

             texto.append("^XZ")
             os.write(texto.toString().toByteArray())

         } catch (ignored: java.lang.Exception) {
         }
         // Pausa
         SystemClock.sleep(500)
     }

     private fun imprFPagoZPL(): String {
         val sImpte: String
         val sCobrado: String
         val sPdte: String
         val lineaSimple: java.lang.StringBuilder = java.lang.StringBuilder()
         val lImptes = 9
         val result: java.lang.StringBuilder = java.lang.StringBuilder()

         if (fDocumento.fSerieExenta) {
             result.append("^XA^LL110").append(fCR).append(fLF)
             result.append("^FT0,0^AKN,20")
             result.append("^FDInversión del sujeto pasivo" + "^FS").append(fCR).append(fLF)
         }

         result.append("^XA^LL110").append(fCR).append(fLF)
         result.append("^FT0,0^AKN,20")
         result.append("^FDForma de pago: " + fFormasPago.getDescrFPago(fDocumento.fPago) + "^FS").append(fCR).append(fLF)

         if (fPendiente.abrirDocumento()) {
             for (x in 0..39) {
                 lineaSimple.append("-")
             }
             result.append("^FT0,30^AKN,30")
             result.append("^FD$lineaSimple^FS").append(fCR).append(fLF)
             result.append("^FT0,50^AKN,20")
             result.append("^FDIMPORTE^FS").append(fCR).append(fLF)
             result.append("^FT115,50^AKN,20")
             result.append("^FDENTREGADO^FS").append(fCR).append(fLF)
             result.append("^FT250,50^AKN,20")
             result.append("^FDPENDIENTE^FS").append(fCR).append(fLF)

             result.append("^FT0,70^AKN,30")
             result.append("^FD$lineaSimple^FS").append(fCR).append(fLF)

             val dImporte: Double = fPendiente.importe.toDouble()
             sImpte = String.format(fFtoImpII, dImporte)
             val dCobrado: Double = fPendiente.cobrado.toDouble()
             sCobrado = String.format(fFtoImpII, dCobrado)
             val dPdte = dImporte - dCobrado
             sPdte = String.format(fFtoImpII, dPdte)

             result.append("^FT0,90^AKN,20")
             result.append("^FD").append(ajustarCadena(sImpte, lImptes, false)).append("^FS").append(fCR).append(fLF)
             result.append("^FT140,90^AKN,20")
             result.append("^FD").append(ajustarCadena(sCobrado, lImptes, false)).append("^FS").append(fCR).append(fLF)
             result.append("^FT280,90^AKN,20")
             result.append("^FD").append(ajustarCadena(sPdte, lImptes, false)).append("^FS").append(fCR).append(fLF)
         }
         result.append("^XZ")

         return result.toString()
     }

     private fun imprPieZPL(): String {
         val lineasPieDoc = fConfiguracion.lineasPieDocZebra(fDocumento.fEmpresa)
         val result: java.lang.StringBuilder = java.lang.StringBuilder()

         result.append("^XA^LL120").append(fCR).append(fLF)
         result.append("^FD").append(lineasPieDoc).append("^FS").append(fCR).append(fLF)

         // En albaranes imprimimos las observaciones del documento.
         if (fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
             result.append("^FT0,60^AKN,20")
             result.append("^FD").append(fDocumento.fObs1).append("^FS").append(fCR).append(fLF)
             result.append("^FT0,90^AKN,20")
             result.append("^FD").append(fDocumento.fObs2).append("^FS").append(fCR).append(fLF)
         }
         result.append("^XZ")

         return result.toString()
     }



     private fun imprBases80ZPL(os: OutputStream) {
         val texto = java.lang.StringBuilder()
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

         var primeraLinea = true
         var y = 0

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

                     texto.append("^FT0,$y^AKN,20")
                     texto.append("^FD").append(ajustarCadena(sBruto, lBruto, false)).append("^FS").append(fCR).append(fLF)
                     texto.append("^FT110,$y^AKN,20")
                     texto.append("^FD").append(ajustarCadena(sImpDto, lImpDto, false)).append("^FS").append(fCR).append(fLF)
                     texto.append("^FT200,$y^AKN,20")
                     texto.append("^FD").append(ajustarCadena(sBase, lBase, false)).append("^FS").append(fCR).append(fLF)
                     texto.append("^FT310,$y^AKN,20")
                     texto.append("^FD").append(ajustarCadena(sPorcIva, 7, false)).append("^FS").append(fCR).append(fLF)
                     texto.append("^FT420,$y^AKN,20")
                     texto.append("^FD").append(ajustarCadena(sImpIva, lImpIva, false)).append("^FS").append(fCR).append(fLF)

                     if (primeraLinea) {
                         y += 40
                         primeraLinea = false
                     }
                     else y += 25
                 }
             }

             var sLineas = java.lang.StringBuilder()
             y += 20
             sLineas.append("^XA^LL$y").append(fCR).append(fLF)
             sLineas.append(texto)
             sLineas.append("^XZ")
             os.write(sLineas.toString().toByteArray())

             // Imprimimos ahora los recargos de equivalencia
             if (fHayRecargo) imprRecEquiv80(os)
             sTotal = String.format(fFtoImpII, fDocumento.fBases.totalConImptos)

             sLineas = java.lang.StringBuilder()
             sLineas.append("^XA^LL$60").append(fCR).append(fLF)
             sLineas.append("^FT0,0^AKN,20")
             sLineas.append("^FDTOTAL IMPORTE: ^FS")
             sLineas.append("^FT350,0^AKN,40")
             sLineas.append("^FD").append(ajustarCadena(sTotal, lTotal, false)).append("^FS").append(fCR).append(fLF)
             sLineas.append("^XZ")
             os.write(sLineas.toString().toByteArray())

             // Pausa
             SystemClock.sleep(500)
         } catch (ignored: java.lang.Exception) {
         }
     }



     private fun imprimirLinea(result: java.lang.StringBuilder, y: Int, sCant: String) {
         val sCodigo = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("codigo"))
         val sDescr = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("descr"))
         val sPrecio = dimePrecioLinea()
         val sImpte = dimeImpteLinea()

         val lCodigo = 7
         val lDescr = 18
         val lCant = 6
         val lPrecio = 6
         val lImpte = 7

         // Código
         result.append("^FT0,$y^AKN,20")
         result.append("^FD").append(ajustarCadena(sCodigo, lCodigo, true)).append("^FS").append(fCR).append(fLF)
         // Descripción
         result.append("^FT90,$y^AKN,20")
         result.append("^FD").append(ajustarCadena(sDescr, lDescr, true)).append("^FS").append(fCR).append(fLF)
         // Cantidad
         result.append("^FT330,$y^AKN,20")
         result.append("^FD").append(ajustarCadena(sCant, lCant, false)).append("^FS").append(fCR).append(fLF)
         // Precio
         result.append("^FT420,$y^AKN,20")
         result.append("^FD").append(ajustarCadena(sPrecio, lPrecio, false)).append("^FS").append(fCR).append(fLF)
         // Importe
         result.append("^FT500,$y^AKN,20")
         result.append("^FD").append(ajustarCadena(sImpte, lImpte, false)).append("^FS").append(fCR).append(fLF)
     }


     private fun imprimirSigLineas(result: java.lang.StringBuilder, y: Int, sCajas: String): Int {
         val sDto: String
         val sLote: String
         val fIncidencia: Int
         val lCajas = 6
         val lLote = 20

         var x = y
         // Si la línea tiene cajas las imprimimos
         if (fDocumento.cLineas.getDouble(fDocumento.cLineas.getColumnIndex("cajas")) != 0.0) {
             x += 20
             result.append("^FT0,$x^AKN,20")
             result.append("^FD").append("Cajas: ").append(ajustarCadena(sCajas, lCajas, false)).append("^FS")
             result.append(fCR).append(fLF)
         }
         // Si la línea tiene descuento lo imprimimos
         if (fDocumento.cLineas.getDouble(fDocumento.cLineas.getColumnIndex("dto")) != 0.0) {
             sDto = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("dto"))
             x += 20
             result.append("^FT0,$x^AKN,20")
             result.append("^FD").append("% dto.: ").append(ajustarCadena(sDto, 5, false)).append("^FS")
             result.append(fCR).append(fLF)
         }
         // Si la línea tiene número de lote lo imprimimos.
         if (fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("lote")) != null
             && fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("lote")) != ""
         ) {
             sLote = fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("lote"))
             x += 20
             result.append("^FT0,$x^AKN,20")
             result.append("^FD").append("Numero lote: ").append(ajustarCadena(sLote, lLote, true)).append("^FS")
             result.append(fCR).append(fLF)
         }
         // Si la línea tiene incidencia la imprimimos
         if (fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("incidencia")) > 0) {
             fIncidencia = fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("incidencia"))
             val tiposIncDao = MyDatabase.getInstance(fContexto)?.tiposIncDao()
             val queDescrInc = tiposIncDao?.dimeDescripcion(fIncidencia) ?: ""
             if (queDescrInc != "") {
                 x += 20
                 result.append("^FT0,$x^AKN,20")
                 result.append("^FD").append("Incidencia: ").append(fIncidencia).append(" ").append(queDescrInc).append("^FS")
                 result.append(fCR).append(fLF)
             }
         }

         return x
     }

     private fun dimePrecioLinea(): String {
         var sPrecio: String = if (fVtaIvaIncluido) {
             fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("precioii")).replace(",", ".")
         } else {
             fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("precio")).replace(",", ".")
         }

         val dPrecio = sPrecio.toDouble()
         // Si la línea es sin cargo lo indicamos
         sPrecio = if (fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("flag")) and FLAGLINEAVENTA_SIN_CARGO > 0) {
             "SIN"
         } else {
             String.format(fFtoPrBase, dPrecio)
         }

         return sPrecio
     }

     private fun dimeImpteLinea(): String {
         var sImpte: String = if (fVtaIvaIncluido) {
             fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("importeii")).replace(",", ".")
         } else {
             fDocumento.cLineas.getString(fDocumento.cLineas.getColumnIndex("importe")).replace(",", ".")
         }

         val dImpte = sImpte.toDouble()
         // Si la línea es sin cargo lo indicamos
         sImpte = if (fDocumento.cLineas.getInt(fDocumento.cLineas.getColumnIndex("flag")) and FLAGLINEAVENTA_SIN_CARGO > 0) {
             "CARGO"
         } else {
             String.format(fFtoImpBase, dImpte)
         }

         return sImpte
     }


     private fun imprRecEquiv80(os: OutputStream) {
         val texto = java.lang.StringBuilder()
         var sPorcRe: String
         var sImpRe: String
         val lImpRe = 7

         texto.append(fCR).append(fLF)
         try {
             for (x in fDocumento.fBases.fLista) {
                 if (x.fBaseImponible != 0.0) {
                     sPorcRe = String.format(fFtoImpBase, x.fPorcRe)
                     sImpRe = String.format(fFtoImpBase, x.fImporteRe)
                     if (x.fImporteRe != 0.0) {
                         texto.append("% Rec. equ.: ").append(ajustarCadena(sPorcRe, 5, false))
                         texto.append(StringOfChar(" ", 3)).append("Rec. equ.: ")
                             .append(ajustarCadena(sImpRe, lImpRe, false))
                             .append(fCR).append(fLF)
                     }
                 }
             }
             os.write(texto.toString().toByteArray())
         } catch (ignored: java.lang.Exception) {
         }
     }


     private fun ajustarCadena(cCadena: String, maxLong: Int, fPorLaDerecha: Boolean): String {
         var result = cCadena
         // Si la cadena supera el máximo de caracteres, la recortamos. En cambio, si no llega a esta cifra, le añadimos espacios al final.
         if (result.length > maxLong) result =
             result.substring(0, maxLong) else if (result.length < maxLong) {
             result =
                 if (fPorLaDerecha) result + StringOfChar(" ", maxLong - result.length)
                 else StringOfChar(" ", maxLong - result.length) + result
         }
         return result
     }


 }