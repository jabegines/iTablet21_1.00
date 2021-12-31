package es.albainformatica.albamobileandroid.comunicaciones

import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.ventas.NotasClientes
import android.widget.TextView
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.Spannable
import android.content.Intent
import android.preference.PreferenceManager
import android.widget.Toast
import android.text.Html
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import es.albainformatica.albamobileandroid.*
import kotlin.Throws
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.util.Base64
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 11/10/13.
 */
class Enviar: AppCompatActivity() {
    private lateinit var fClientes: ClientesClase
    private lateinit var fNotas: NotasClientes
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fNumExportaciones: NumExportaciones

    private lateinit var puente: Handler
    private lateinit var tvDatos: TextView
    private lateinit var btnEnvFTP: Button
    private lateinit var btnEnvDatos: Button

    private var fTerminar: Boolean = false
    private lateinit var thread: Thread
    private lateinit var prefs: SharedPreferences
    private var fCodTerminal: String = ""
    private var usarMultisistema: Boolean = false
    private lateinit var aCabeceras: ArrayList<Int>

    // Request de las actividades a las que llamamos.
    private val fRequestNumExport = 0


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.enviar)
        aCabeceras = ArrayList()
        // Utilizaremos fTerminar para terminar el thread antes de tiempo.
        fTerminar = false
        fClientes = ClientesClase(this)
        fNotas = NotasClientes(this)
        fConfiguracion = Comunicador.fConfiguracion
        fNumExportaciones = NumExportaciones(this)
        inicializarControles()
    }

    override fun onDestroy() {
        aCabeceras.clear()
        fClientes.close()
        fNotas.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_enviar, menu)

        // Configuramos cada uno de los items del menu, dandole un color personalizado (azul de la aplicacion).
        val text = SpannableStringBuilder()
        text.append(resources.getString(R.string.mni_numeroexp))
        text.setSpan(
            ForegroundColorSpan(Color.parseColor(COLOR_MENUS)),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val item = menu.findItem(R.id.mni_numexportac)
        item.title = text
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i: Intent
        return when (item.itemId) {
            R.id.mni_numexportac -> {
                i = Intent(this, PedirNumExport::class.java)
                startActivityForResult(i, fRequestNumExport)
                true
            }
            else -> true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestNumExport) {
            if (resultCode == RESULT_OK) {
                val iQueNumExp = data?.getIntExtra("numexport", 0) ?: 0
                if (iQueNumExp > 0) {
                    baseDatosAXML(iQueNumExp)
                }
            }
        }
    }

    private fun inicializarControles() {
        tvDatos = findViewById(R.id.tvEnv_Datos)
        btnEnvFTP = findViewById(R.id.btnEnvFTP)
        btnEnvDatos = findViewById(R.id.btnEnvWifi)
        tvDatos.text = ""

        // Leemos las preferencias de la aplicacion;
        prefs = PreferenceManager.getDefaultSharedPreferences(this@Enviar)
        fCodTerminal = prefs.getString("terminal", "") ?: ""
        if (fCodTerminal == "") fCodTerminal = fConfiguracion.codTerminal()

        // Si no tenemos un codigo de terminal correcto (3 digitos numericos), abandonamos.
        if (!codTerminalCorrecto(fCodTerminal)) {
            val nuevoToast =
                Toast.makeText(this, "EL CODIGO DEL TERMINAL NO ES CORRECTO", Toast.LENGTH_LONG)
            nuevoToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            nuevoToast.show()
            finish()
        }
        usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        puente = object : Handler() {
            override fun handleMessage(msg: Message) {
                tvDatos.setText(
                    tvDatos.getText().toString() + Html.fromHtml("<br />") + msg.obj.toString()
                )
            }
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.enviar)
        val toolbar = findViewById<Toolbar>(R.id.tlbAlba)
        setSupportActionBar(toolbar)
    }

    fun prepararDatos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val aldDialog = NuevoAlertBuilder(
            this, resources.getString(R.string.tit_prepdatos),
            resources.getString(R.string.dlg_datosenvio), true
        )
        aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _, _ ->
            baseDatosAXML(0)
        }
        val alert = aldDialog.create()
        alert.show()
    }

    fun enviarFTP(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val aldDialog = NuevoAlertBuilder(
            this, resources.getString(R.string.tit_envioftp),
            resources.getString(R.string.dlg_envioftp), true
        )
        aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _, _ -> comenzarEnvioFTP() }
        val alert = aldDialog.create()
        alert.show()
    }

    fun enviarWifi(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val aldDialog = NuevoAlertBuilder(
            this, resources.getString(R.string.tit_enviowifi),
            resources.getString(R.string.dlg_enviowifi), true
        )
        aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _, _ -> comenzarEnvioWIFI() }
        val alert = aldDialog.create()
        alert.show()
    }

    private fun baseDatosAXML(queNumExportacion: Int) {
        // Deshabilitamos los botones para que no podamos interrumpir el proceso de envio.
        btnEnvFTP.isEnabled = false
        btnEnvDatos.isEnabled = false
        val miscCom = MiscComunicaciones(this, false)
        miscCom.puente = puente
        miscCom.baseDatosAXML(queNumExportacion)
        aCabeceras = miscCom.aCabeceras
        val queTexto = tvDatos.text.toString() + Html.fromHtml("<br />") + resources.getString(
            R.string.msj_FinPrep
        )
        tvDatos.text = queTexto

        // Volvemos a habilitar los botones.
        btnEnvFTP.isEnabled = true
        btnEnvDatos.isEnabled = true
    }

    private fun comenzarEnvioWIFI() {
        val localDirectory = dimeRutaEnvLocal()
        tvDatos.text = ""
        thread = Thread(object : Runnable {
            var rutaLocalEnvio = File(localDirectory)
            override fun run() {
                Looper.prepare()

                // Vemos las carpetas de comunicacion que tenemos en preferencias.
                var rutaWifi = prefs.getString("ruta_wifi", "") ?: ""
                if (rutaWifi != "") {
                    val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
                    rutaWifi =
                        if (usarMultisistema) rutaWifi + "/" + fCodTerminal + dimeRutaEnvSistema() + "/Recepcion/" else "$rutaWifi/$fCodTerminal/Recepcion/"
                    try {
                        // Lo primero que hacemos es comprobar si hay ficheros para enviar.
                        val xmlFiles = rutaLocalEnvio.listFiles()
                        if (xmlFiles != null && xmlFiles.isNotEmpty()) {
                            val rWifi = RedWifi(this@Enviar)
                            if (rWifi.enviar(rutaWifi, xmlFiles, puente)) {
                                // Si hemos ido guardando las imágenes con las firmas digitales, las enviamos.
                                if (fConfiguracion.activarFirmaDigital() || fConfiguracion.hayReparto()) {
                                    enviarFirmas(rWifi, rutaWifi)
                                }
                                // Una vez hemos enviado, borramos los ficheros de la carpeta local.
                                val msg = Message()
                                msg.obj = resources.getString(R.string.msj_BorrFich)
                                puente.sendMessage(msg)
                                for (File in xmlFiles) {
                                    File.delete()
                                }
                                val msgFin = Message()
                                msgFin.obj = resources.getString(R.string.msj_FinEnvio)
                                puente.sendMessage(msgFin)

                                // Guardamos también en las preferencias la fecha y hora del último envío.
                                val tim = System.currentTimeMillis()
                                val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                val fFecha = df.format(tim)
                                prefs.edit().putString("fecha_ult_envio", fFecha).apply()
                            }
                        } else {
                            val msgSinFich = Message()
                            msgSinFich.obj = resources.getString(R.string.msj_SinFich_RutaEnv)
                            puente.sendMessage(msgSinFich)
                        }
                    } catch (e: Exception) {
                        val msgExcept = Message()
                        msgExcept.obj = e.message
                        puente.sendMessage(msgExcept)
                    }
                } else MsjAlerta(this@Enviar).alerta(resources.getString(R.string.msj_SinRutaWifi))
                Looper.loop()
            }
        })
        thread.start()
    }

    @Throws(Exception::class)
    private fun enviarFirmas(rWifi: RedWifi, rutaWifi: String) {
        val dirlocFirmas = dimeRutaLocalFirmas()
        val rutaLocalFirmas = File(dirlocFirmas)
        val firmasFiles = rutaLocalFirmas.listFiles { _, name -> nombreFirmaValido(name) }
        if (firmasFiles != null && firmasFiles.isNotEmpty()) {
            rWifi.enviar(rutaWifi, firmasFiles, puente)
            // Borramos los ficheros de firmas de la carpeta local.
            val msg = Message()
            msg.obj = resources.getString(R.string.msj_BorrFich)
            puente.sendMessage(msg)
            for (fichFirma in firmasFiles) {
                fichFirma.delete()
            }
        }
    }

    @Throws(Exception::class)
    private fun enviarFirmasFtp(ftpClient: FTPClient, msjEnv: String) {
        val dirlocFirmas = dimeRutaLocalFirmas()
        val rutaLocalFirmas = File(dirlocFirmas)
        val firmasFiles = rutaLocalFirmas.listFiles { _, name -> nombreFirmaValido(name) }
        // Encriptamos los ficheros de firmas
        for (file in firmasFiles) {
            val encodedBytes = Base64.encodeBase64(loadFileAsBytesArray(dirlocFirmas + file.name))
            writeByteArraysToFile(dirlocFirmas + file.name + ".txt", encodedBytes)
        }
        // Volvemos a leer en un array los ficheros .txt, que son los que enviaremos
        val firmasTxt = rutaLocalFirmas.listFiles { _, name -> name.endsWith("txt") }
        try {
            for (file in firmasTxt) {
                if (fTerminar) break
                val msg = Message()
                msg.obj = msjEnv + file.name
                puente.sendMessage(msg)
                val buffIn = BufferedInputStream(FileInputStream(file))
                ftpClient.storeFile(file.name, buffIn)
                buffIn.close()
                file.delete()
            }
            // Borramos también los ficheros .jpg
            for (fichFirma in firmasFiles) {
                fichFirma.delete()
            }
        } catch (e: IOException) {
            val msgExcept = Message()
            msgExcept.obj = e.message
            puente.sendMessage(msgExcept)
        }
    }

    private fun nombreFirmaValido(name: String): Boolean {
        // Buscamos si en el array de aCabeceras está el documento al que pertenece la firma, para enviarla o no.
        var resultado = false
        var queFichero: String
        for (idDoc in aCabeceras) {
            queFichero = "$idDoc.jpg"
            if (name.equals(queFichero, ignoreCase = true)) {
                resultado = true
                break
            }
        }
        return resultado
    }

    private fun comenzarEnvioFTP() {
        val localDirectory = dimeRutaEnvLocal()
        // Limpiamos tvDatos.
        tvDatos.text = ""
        thread = Thread(object : Runnable {
            var fServidor = fConfiguracion.servidorFTP()
            var fUsuario = fConfiguracion.usuarioFTP()
            var fPassword = fConfiguracion.passwordFTP()
            var msjEnv = resources.getString(R.string.msj_EnviandoFich)
            var rutaLocalEnvio = File(localDirectory)
            override fun run() {
                try {
                    // Lo primero que hacemos es comprobar si hay ficheros para enviar.
                    val xmlFiles = rutaLocalEnvio.listFiles()
                    if (xmlFiles != null && xmlFiles.isNotEmpty()) {
                        val ftpClient = FTPClient()
                        ftpClient.connect(fServidor)
                        val login = ftpClient.login(fUsuario, fPassword)
                        if (login) {
                            // Entramos en modo pasivo.
                            ftpClient.enterLocalPassiveMode()
                            // Intentamos cambiar a la carpeta de exportacion.
                            if (crearCarpetaExportFTP(ftpClient)) {

                                // Bucle que recorre la lista de ficheros.
                                for (file in xmlFiles) {
                                    // Controlamos que queremos salir del thread.
                                    if (fTerminar) break
                                    // Mandamos el mensaje al Handler a traves de un nuevo Message.
                                    val msg = Message()
                                    msg.obj = msjEnv + file.name
                                    puente.sendMessage(msg)

                                    // Creamos el stream de entrada.
                                    val buffIn = BufferedInputStream(FileInputStream(file))
                                    // Subimos el fichero actual de la lista.
                                    ftpClient.storeFile(file.name, buffIn)
                                    // Cerramos el stream de entrada.
                                    buffIn.close()

                                    // Borramos el fichero de la carpeta.
                                    file.delete()
                                }
                                // Si hemos ido guardando las imágenes con las firmas digitales, las enviamos.
                                if (fConfiguracion.activarFirmaDigital() || fConfiguracion.hayReparto()) {
                                    try {
                                        enviarFirmasFtp(ftpClient, msjEnv)
                                    } catch (e: Exception) {
                                        val msgExcept = Message()
                                        msgExcept.obj = e.message
                                        puente.sendMessage(msgExcept)
                                    }
                                }
                            }
                        }
                        ftpClient.logout()
                        ftpClient.disconnect()
                        if (!fTerminar) {
                            val msgFin = Message()
                            msgFin.obj = resources.getString(R.string.msj_FinEnvio)
                            puente.sendMessage(msgFin)

                            // Guardamos también en las preferencias la fecha y hora del último envío.
                            val tim = System.currentTimeMillis()
                            val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            val fFecha = df.format(tim)
                            prefs.edit().putString("fecha_ult_envio", fFecha).apply()
                        }
                    } else {
                        val msgSinFich = Message()
                        msgSinFich.obj = "No se encontraron ficheros en la carpeta de envío"
                        puente.sendMessage(msgSinFich)
                    }
                } catch (e: IOException) {
                    val msgExcept = Message()
                    msgExcept.obj = e.message
                    puente.sendMessage(msgExcept)
                }
            }
        })
        thread.start()
    }

    private fun dimeRutaEnvLocal(): String {
        val result: String
        // Vemos la carpeta de envio que tenemos en preferencias.
        val directorioLocal = prefs.getString("rutacomunicacion", "") ?: ""
        result = if (directorioLocal == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/envio/" + fCodTerminal + "/" + BaseDatos.queBaseDatos else "/storage/sdcard0/alba/envio/$fCodTerminal"
        } else {
            if (usarMultisistema) directorioLocal + "/envio/" + fCodTerminal + "/" + BaseDatos.queBaseDatos else "$directorioLocal/envio/$fCodTerminal"
        }
        return result
    }

    private fun dimeRutaLocalFirmas(): String {
        val result: String
        // Vemos la carpeta de envio que tenemos en preferencias.
        val directorioLocal = prefs.getString("rutacomunicacion", "") ?: ""
        result = if (directorioLocal == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/firmas/" + BaseDatos.queBaseDatos else "/storage/sdcard0/alba/firmas/"
        } else {
            if (usarMultisistema) directorioLocal + "/firmas/" + BaseDatos.queBaseDatos else "$directorioLocal/firmas/"
        }
        return result
    }

    private fun crearCarpetaExportFTP(ftpClient: FTPClient): Boolean {
        var continuar = true
        var fNombreCarpeta = "/" + fConfiguracion.carpetaExportFTP()
        return try {
            if (!ftpClient.changeWorkingDirectory("/$fNombreCarpeta")) {
                ftpClient.makeDirectory(fNombreCarpeta)
                continuar = ftpClient.changeWorkingDirectory("/$fNombreCarpeta")
            }
            if (continuar) {
                fNombreCarpeta = "$fNombreCarpeta/$fCodTerminal"
                if (!ftpClient.changeWorkingDirectory(fNombreCarpeta)) {
                    ftpClient.makeDirectory(fNombreCarpeta)
                    continuar = ftpClient.changeWorkingDirectory(fNombreCarpeta)
                }
                if (continuar) {
                    if (usarMultisistema) {
                        fNombreCarpeta += dimeRutaEnvSistema()
                        if (!ftpClient.changeWorkingDirectory(fNombreCarpeta)) {
                            ftpClient.makeDirectory(fNombreCarpeta)
                            continuar = ftpClient.changeWorkingDirectory(fNombreCarpeta)
                        }
                    }
                }
                if (continuar) {
                    // Obtenemos la fecha actual.
                    val tim = System.currentTimeMillis()
                    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fFecha = df.format(tim)
                    val dfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val fHora = dfHora.format(tim)
                    val nombreCarpeta =
                        (fFecha.substring(6, 10) + fFecha.substring(3, 5) + fFecha.substring(0, 2)
                                + fHora.substring(0, 2) + fHora.substring(3, 5) + fHora.substring(
                            6,
                            8
                        ))
                    ftpClient.makeDirectory("$fNombreCarpeta/$nombreCarpeta")
                    return ftpClient.changeWorkingDirectory("$fNombreCarpeta/$nombreCarpeta")
                }
            }
            false
        } catch (e: IOException) {
            false
        }
    }

    fun salir(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        finish()
    }

    private fun dimeRutaEnvSistema(): String {
        when (BaseDatos.queBaseDatos) {
            "DBAlba00" -> return "/ALBADB00"
            "DBAlba10" -> return "/ALBADB10"
            "DBAlba20" -> return "/ALBADB20"
            "DBAlba30" -> return "/ALBADB30"
            "DBAlba40" -> return "/ALBADB40"
            "DBAlba50" -> return "/ALBADB50"
            "DBAlba60" -> return "/ALBADB60"
            "DBAlba70" -> return "/ALBADB70"
            "DBAlba80" -> return "/ALBADB80"
            "DBAlba90" -> return "/ALBADB90"
        }
        return ""
    }


    @Throws(IOException::class)
    fun writeByteArraysToFile(fileName: String, content: ByteArray) {
        val file = File(fileName)
        val writer = BufferedOutputStream(FileOutputStream(file))
        writer.write(content)
        writer.flush()
        writer.close()
    }


    @Throws(Exception::class)
    fun loadFileAsBytesArray(fileName: String): ByteArray {
        val file = File(fileName)
        val length = file.length().toInt()
        val reader = BufferedInputStream(FileInputStream(file))
        val bytes = ByteArray(length)
        reader.read(bytes, 0, length)
        reader.close()
        return bytes
    }

}