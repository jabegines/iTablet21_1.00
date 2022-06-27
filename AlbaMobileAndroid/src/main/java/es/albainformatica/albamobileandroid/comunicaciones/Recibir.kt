package es.albainformatica.albamobileandroid.comunicaciones

import android.app.Activity
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.Manifest.permission
import android.content.pm.PackageManager
import android.view.Gravity
import android.content.Intent
import android.content.DialogInterface
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.CargasDao
import es.albainformatica.albamobileandroid.dao.CobrosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.util.Base64
import java.io.*
import java.lang.Exception

/**
 * Created by jabegines on 9/10/13.
 */
class Recibir : Activity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var puente: Handler
    private lateinit var fRegEventos: RegistroEventosClase

    private lateinit var btnRecFTP: Button
    private lateinit var btnRecWifi: Button
    private lateinit var chkRecibirImg: CheckBox
    private lateinit var chkRecibirDocAs: CheckBox

    private var fTerminar: Boolean = false
    private lateinit var thread: Thread
    private lateinit var prefs: SharedPreferences

    private var fServidor: String = ""
    private var fUsuario: String = ""
    private var fPassword: String = ""
    private var fCodTerminal: String = ""
    private var fImportando: Boolean = false
    private var usarMultisistema: Boolean = false
    private var fNumArchivo = 1

    private val fRequestPedirConfFtp = 1
    private val fRequestPermisoAlmacenamiento = 2


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.recibir)

        fRegEventos = Comunicador.fRegEventos
        fRegEventos.registrarEvento(codEv_ComRec_Entrar, descrEv_ComRec_Entrar)

        fConfiguracion = Comunicador.fConfiguracion

        // Leemos las preferencias de la aplicación;
        prefs = PreferenceManager.getDefaultSharedPreferences(this@Recibir)
        // Necesito esto aquí para usarlo en hayFichPreparados().
        fCodTerminal = prefs.getString("terminal", "") ?: ""
        usarMultisistema = prefs.getBoolean("usar_multisistema", false)

        // Utilizaremos fTerminar para terminar el thread antes de tiempo.
        fTerminar = false
        fImportando = false
        // Comprobamos que podemos recibir: si tenemos algún documento pendiente de enviar no recibiremos.
        // Por otro lado, no preguntaremos si vaciamos la base de datos, lo haremos siempre.
        inicializarControles(puedoRecibir())

        // Comprobamos si tenemos el permiso para instalar desde fuentes desconocidas (a partir de la api 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {

                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:es.albainformatica.albamobileandroid")
                    )
                )
            }
        }

        // Comprobamos si tenemos el permiso de almacenamiento para, en su caso, pedirlo
        val permissionCheck = ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission.WRITE_EXTERNAL_STORAGE),
                fRequestPermisoAlmacenamiento
            )
        }

    }

    override fun onDestroy() {
        detenerThread()
        fRegEventos.registrarEvento(codEv_ComRec_Salir, descrEv_ComRec_Salir)

        super.onDestroy()
    }

    // No podremos recibir siempre que:
    // - tengamos documentos o cobros pendientes de enviar
    // - tengamos ficheros preparados y no enviados
    // - tengamos cargas
    // - la fecha del último envío desde el terminal sea mayor que la de la última preparación desde el PC.
    private fun puedoRecibir(): Boolean {
        var resultado = true

        // Puede ocurrir que no tengamos aún la base de datos creada, por eso tenemos que controlar la excepción.
        try {
            val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(this)?.cabecerasDao()
            val queCabId = cabecerasDao?.hayDocsParaEnviar() ?: 0
            if (queCabId > 0) {
                MsjAlerta(this).alerta("Tiene documentos pendientes de enviar. No podrá recibir.")
                resultado = false
            }
            // Vemos si tenemos algún cobro
            val cobrosDao: CobrosDao? = MyDatabase.getInstance(this)?.cobrosDao()
            val lCobros = cobrosDao?.abrirParaExportar() ?: emptyList<CobrosEnt>().toMutableList()
            if (lCobros.isNotEmpty()) {
                MsjAlerta(this).alerta("Tiene cobros pendientes de enviar. No podrá recibir.")
                resultado = false
            }
            // Vemos si tenemos alguna carga
            if (resultado) {
                val cargasDao: CargasDao? = MyDatabase.getInstance(this)?.cargasDao()
                val lCargas = cargasDao?.getPdtesEnviar() ?: emptyList<Int>().toMutableList()
                if (lCargas.isNotEmpty()) {
                    MsjAlerta(this).alerta("Tiene cargas pendientes de enviar. No podrá recibir.")
                    resultado = false
                }
            }
            // Vemos si hay ficheros preparados
            if (resultado) {
                // Comprobaremos las fechas más adelante.
                if (hayFichPreparados()) {
                    MsjAlerta(this).alerta("Tiene documentos preparados pendientes de enviar. No podrá recibir.")
                    resultado = false
                }
            }
        } catch (e: Exception) {
            resultado = true
        }
        return resultado
    }

    private fun hayFichPreparados(): Boolean {
        var queRutaEnv = prefs.getString("rutacomunicacion", "") ?: ""
        queRutaEnv = if (queRutaEnv == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/envio/$fCodTerminal/$queBDRoom"
            else "/storage/sdcard0/alba/envio/$fCodTerminal"
        } else {
            if (usarMultisistema) "$queRutaEnv/envio/$fCodTerminal/$queBDRoom"
            else "$queRutaEnv/envio/$fCodTerminal"
        }
        val sFichero = queRutaEnv
        val fichero = File(sFichero)
        val ficheros = fichero.listFiles()
        return ficheros?.isNotEmpty() ?: false
    }

    private fun inicializarControles(puedoRecibir: Boolean) {
        val tvRecibiendo = findViewById<TextView>(R.id.tvRecibiendo)
        val tvImportando = findViewById<TextView>(R.id.tvImportando)
        val tvPorcentaje = findViewById<TextView>(R.id.tvPorcentaje)
        val tvNumArchivos = findViewById<TextView>(R.id.tvNumArchivos)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        tvRecibiendo.text = getString(R.string.msj_RecibiendoFich)
        tvImportando.text = getString(R.string.msj_ImportandoFich)
        tvPorcentaje.text = "0%"
        tvNumArchivos.text = "0/0"
        //progressBar.setVisibility(View.GONE);
        btnRecFTP = findViewById(R.id.btnRecFTP)
        btnRecWifi = findViewById(R.id.btnRecWifi)
        chkRecibirImg = findViewById(R.id.chkRecibirImag)
        chkRecibirDocAs = findViewById(R.id.chkRecibirDocAs)

        // Configuramos los parámetros, tanto para el FTP como para la recepción vía WIFI.
        configuracionTerminal()
        puente = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.arg1 > 0) {
                    tvImportando.text = msg.obj.toString()
                    if (progressBar.max == 0) {
                        progressBar.max = msg.arg1
                        progressBar.visibility = View.VISIBLE
                    }
                    progressBar.incrementProgressBy(1)
                    val sPorcentaje = msg.arg2.toString() + "%"
                    tvPorcentaje.text = sPorcentaje
                    val sNumArchivos = fNumArchivo.toString() + "/" + msg.arg1
                    tvNumArchivos.text = sNumArchivos
                    fNumArchivo++
                } else {
                    tvRecibiendo.text = msg.obj.toString()
                }
            }
        }
        if (!puedoRecibir) {
            chkRecibirImg.isEnabled = false
            chkRecibirDocAs.isEnabled = false
            btnRecWifi.isEnabled = false
            btnRecFTP.isEnabled = false
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.recibir)
    }

    private fun configuracionTerminal() {
        //fCodTerminal = pref.getString("terminal", "");
        // Si no tenemos un código de terminal correcto (3 dígitos numéricos), abandonamos.
        if (!codTerminalCorrecto(fCodTerminal)) {
            val nuevoToast =
                Toast.makeText(this, getString(R.string.msj_CodTermIncorr), Toast.LENGTH_LONG)
            nuevoToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            nuevoToast.show()
            finish()
        }
        if (!fConfiguracion.servidorFTP().equals("", ignoreCase = true)) {
            fServidor = fConfiguracion.servidorFTP()
            fUsuario = fConfiguracion.usuarioFTP()
            fPassword = fConfiguracion.passwordFTP()
        }
    }

    fun recibirWifi(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val aldDialog = nuevoAlertBuilder(
            this, resources.getString(R.string.tit_recepwifi),
            resources.getString(R.string.dlg_recepwifi), true
        )
        aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _: DialogInterface?, _: Int -> comenzarRecepcionWIFI() }
        val alert = aldDialog.create()
        alert.show()
    }


    private fun detenerThread() {
        fTerminar = true
    }


    fun recibirFTP(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fConfiguracion.servidorFTP().equals("", ignoreCase = true)) {
            val i = Intent(this, PedirConfigFtp::class.java)
            startActivityForResult(i, fRequestPedirConfFtp)
        }
        val aldDialog = nuevoAlertBuilder(
            this, resources.getString(R.string.tit_recepftp),
            resources.getString(R.string.dlg_recepftp), true
        )
        aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _: DialogInterface?, _: Int -> comenzarRecepcionFTP() }
        val alert = aldDialog.create()
        alert.show()
    }


    private fun comenzarRecepcionWIFI() {
        fImportando = true
        // Deshabilitamos los botones para que no podamos interrumpir el proceso de recogida.
        btnRecWifi.isEnabled = false
        btnRecFTP.isEnabled = false
        chkRecibirImg.isEnabled = false
        chkRecibirDocAs.isEnabled = false
        thread = Thread {
            Looper.prepare()

            // Vemos las carpetas de comunicacion que tenemos en preferencias.
            var rutaWifi = prefs.getString("ruta_wifi", "")
            if (rutaWifi != null && rutaWifi != "") {
                rutaWifi = if (usarMultisistema) {
                    rutaWifi + "/" + fCodTerminal + dimeRutaEnvSistema() + "/Envio/"
                } else {
                    "$rutaWifi/$fCodTerminal/Envio/"
                }
                var localDirectory = prefs.getString("rutacomunicacion", "")
                localDirectory =
                    if (localDirectory != null && localDirectory == "") "/storage/sdcard0/alba/recepcion/$fCodTerminal" else "$localDirectory/recepcion/$fCodTerminal"

                // Nos aseguramos de que la carpeta existe y, si no, la creamos.
                val rutarecepcion = File(localDirectory)
                if (!rutarecepcion.exists()) rutarecepcion.mkdirs()
                try {
                    val rWifi = RedWifi(this@Recibir)
                    // Comprobamos que el fichero bandera no exista para poder recibir.
                    // Además comprobamos también las fechas de último envío y de última preparación.
                    if (rWifi.puedoRecibirWifi(rutaWifi)) {
                        if (chkRecibirImg.isChecked) recibirImagWifi(rWifi)
                        if (chkRecibirDocAs.isChecked) recibirDocAsoc(rWifi)
                        if (rWifi.recibir(rutaWifi, localDirectory, puente)) {
                            //Message msgFin;
                            //msgFin.obj = getResources().getString(R.string.msj_FinRecepcion);
                            //puente.sendMessage(msgFin);
                            val miscCom = MiscComunicaciones(this@Recibir, false)
                            miscCom.puente = puente
                            miscCom.xmlABaseDatos()

                            //msgFin = new Message();
                            //msgFin.obj = "Fin de la importación";
                            //puente.sendMessage(msgFin);
                            fImportando = false

                            // Reseteamos la aplicación, para no tener problemas con las fecha de
                            // inicio y fin del ejercicio. Así también refrescamos las etiquetas de la pantalla principal.
                            val aldDialog = nuevoAlertBuilder(
                                this@Recibir,
                                "Información",
                                "Fin de la importación",
                                false
                            )
                            aldDialog.setPositiveButton("ACEPTAR") { _: DialogInterface?, _: Int ->
                                val i = baseContext.packageManager.getLaunchIntentForPackage(
                                    baseContext.packageName
                                )
                                if (i != null) {
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(i)
                                }
                            }
                            val alert = aldDialog.create()
                            alert.show()
                        }
                    } else {
                        fImportando = false
                        val msg = Message()
                        msg.obj = getString(R.string.msj_DatosYaRec)
                        puente.sendMessage(msg)
                    }
                } catch (e: Exception) {
                    mostrarExcepcion(e)
                }
            } else MsjAlerta(this@Recibir).alerta(getString(R.string.msj_SinRutaWifi))
            Looper.loop()
        }
        thread.start()
    }

    private fun recibirDocAsoc(rWifi: RedWifi) {
        var rutaWifiDocAs = prefs.getString("ruta_wifi", "") ?: ""
        rutaWifiDocAs = if (usarMultisistema) {
            rutaWifiDocAs + "/" + fCodTerminal + dimeRutaEnvSistema() + "/DocAsociados/"
        } else {
            "$rutaWifiDocAs/$fCodTerminal/DocAsociados/"
        }
        var localDirectory = prefs.getString("rutacomunicacion", "") ?: ""
        localDirectory = if (localDirectory == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/docasociados/$queBDRoom"
            else "/storage/sdcard0/alba/docasociados"
        } else {
            if (usarMultisistema) "$localDirectory/docasociados/$queBDRoom"
            else "$localDirectory/docasociados"
        }
        val rutaLocalDocAs = File(localDirectory)
        // Vaciamos la carpeta de documentos asociados antes de recibir. Así nos aseguramos de que no tenemos
        // documentos viejos que no se usan. Habrá que ver si esto no ralentiza el proceso más de lo aconsejable.
        val xmlFiles = rutaLocalDocAs.listFiles()
        if (xmlFiles != null && xmlFiles.isNotEmpty()) {
            for (file in xmlFiles) {
                file.delete()
            }
        }
        // Nos aseguramos de que la carpeta existe y, si no, la creamos.
        if (!rutaLocalDocAs.exists()) rutaLocalDocAs.mkdirs()
        try {
            rWifi.recibir(rutaWifiDocAs, localDirectory, puente)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }

    private fun recibirImagWifi(rWifi: RedWifi) {
        var rutaWifiImag = prefs.getString("ruta_wifi", "") ?: ""
        rutaWifiImag = if (usarMultisistema) {
            rutaWifiImag + "/" + fCodTerminal + dimeRutaEnvSistema() + "/Imagenes/"
        } else {
            "$rutaWifiImag/$fCodTerminal/Imagenes/"
        }
        try {
            // Si la ruta wifi para las imágenes está vacía no intentaremos recoger, para no perder las imágenes en la tablet
            if (rWifi.hayImagenes(rutaWifiImag)) {
                var localDirectory = prefs.getString("rutacomunicacion", "") ?: ""
                localDirectory = if (localDirectory == "") {
                    if (usarMultisistema) "/storage/sdcard0/alba/imagenes/$queBDRoom"
                    else "/storage/sdcard0/alba/imagenes"
                } else {
                    if (usarMultisistema) "$localDirectory/imagenes/$queBDRoom"
                    else "$localDirectory/imagenes"
                }

                // Nos aseguramos de que la carpeta existe y, si no, la creamos.
                // Borramos el contenido de la carpeta, para no tener imágenes antiguas.
                val rutaLocalImag = File(localDirectory)
                if (rutaLocalImag.exists()) deleteDirectory(rutaLocalImag) else rutaLocalImag.mkdirs()
                try {
                    rWifi.recibir(rutaWifiImag, localDirectory, puente)
                } catch (e: Exception) {
                    mostrarExcepcion(e)
                }
            }
        } catch (e: Exception) {
            //
        }
    }

    private fun deleteDirectory(fileOrDirectory: File) {
        val files = fileOrDirectory.listFiles() ?: emptyArray()
        for (child in files) {
            child.delete()
        }
    }

    private fun comenzarRecepcionFTP() {
        val localDirectory: String
        fImportando = true
        // Deshabilitamos los botones para que no podamos interrumpir el proceso de recogida.
        btnRecWifi.isEnabled = false
        btnRecFTP.isEnabled = false
        chkRecibirImg.isEnabled = false
        chkRecibirDocAs.isEnabled = false

        // Vemos la carpeta de comunicación que tenemos en preferencias.
        val queRuta = prefs.getString("rutacomunicacion", "")
        localDirectory =
            if (queRuta != null && queRuta == "") "/storage/sdcard0/alba/recepcion/$fCodTerminal" else "$queRuta/recepcion/$fCodTerminal"

        // Nos aseguramos de que la carpeta existe y, si no, la creamos.
        val rutarecepcion = File(localDirectory)
        if (!rutarecepcion.exists()) rutarecepcion.mkdirs()
        thread = Thread(object : Runnable {
            val msjRec = resources.getString(R.string.msj_RecibiendoFich)
            override fun run() {
                Looper.prepare()
                try {
                    val ftpClient = FTPClient()
                    ftpClient.connect(fServidor)
                    val login = ftpClient.login(fUsuario, fPassword)
                    val fCarpetaImp: String
                    if (login) {
                        // Establecemos el tamaño del buffer. Si no hacemos esto, la descarga irá muy lenta.
                        ftpClient.bufferSize = 1024 * 1024
                        // Entramos en modo pasivo.
                        ftpClient.enterLocalPassiveMode()
                        // Intentamos cambiar a la carpeta de importacion.
                        fCarpetaImp = if (usarMultisistema) {
                            "/" + fConfiguracion.carpetaImportFTP() + "/" + fCodTerminal + dimeRutaEnvSistema()
                        } else {
                            "/" + fConfiguracion.carpetaImportFTP() + "/" + fCodTerminal
                        }
                        if (ftpClient.changeWorkingDirectory(fCarpetaImp)) {
                            // Construimos la lista con los nombres de los ficheros.
                            val ftpFiles = ftpClient.listFiles()
                            if (ftpFiles != null && ftpFiles.isNotEmpty()) {

                                // Bucle que recorre la lista de ficheros.
                                for (file in ftpFiles) {
                                    if (!file.isFile) continue
                                    // Controlamos que queremos salir del thread.
                                    if (fTerminar) break
                                    // Mandamos el mensaje al Handler a través de un nuevo Message.
                                    val msg = Message()
                                    msg.obj = msjRec + file.name
                                    puente.sendMessage(msg)

                                    // Creamos el stream de salida.
                                    var output: OutputStream
                                    output = FileOutputStream(localDirectory + "/" + file.name)
                                    // Bajamos el fichero actual de la lista.
                                    ftpClient.retrieveFile(file.name, output)
                                    // Cerramos el stream de salida.
                                    output.close()

                                    // Borramos el fichero del servidor.
                                    ftpClient.deleteFile(file.name)
                                }
                                // Vemos si tenemos que recoger imagenes.
                                if (chkRecibirImg.isChecked) recImagenesFTP(ftpClient, msjRec)
                                // Vemos si tenemos que recoger documentos asociados.
                                if (chkRecibirDocAs.isChecked) recDocAsocFTP(ftpClient, msjRec)
                            } else {
                                val msgSinFich = Message()
                                msgSinFich.obj = resources.getString(R.string.msj_SinFich_Servidor)
                                puente.sendMessage(msgSinFich)
                            }
                        } else {
                            val msgNoDirect = Message()
                            msgNoDirect.obj = resources.getString(R.string.msj_NoExisteDirect)
                            puente.sendMessage(msgNoDirect)
                        }
                    }
                    ftpClient.logout()
                    ftpClient.disconnect()
                    if (!fTerminar) {
                        //Message msgFin = new Message();
                        //msgFin.obj = getResources().getString(R.string.msj_FinRecepcion);
                        //puente.sendMessage(msgFin);
                        val miscCom = MiscComunicaciones(this@Recibir, false)
                        miscCom.puente = puente
                        miscCom.xmlABaseDatos()

                        //msgFin = new Message();
                        //msgFin.obj = "Fin de la importación";
                        //puente.sendMessage(msgFin);
                        fImportando = false

                        // Reseteamos la aplicación, para no tener problemas con las fecha de
                        // inicio y fin del ejercicio. Así también refrescamos las etiquetas de la pantalla principal.
                        val aldDialog = nuevoAlertBuilder(
                            this@Recibir,
                            "Información",
                            "Fin de la importación",
                            false
                        )
                        aldDialog.setPositiveButton("ACEPTAR") { _: DialogInterface?, _: Int ->
                            val i = baseContext.packageManager.getLaunchIntentForPackage(
                                baseContext.packageName
                            )
                            if (i != null) {
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(i)
                            }
                        }
                        val alert = aldDialog.create()
                        alert.show()
                    } else fImportando = false
                } catch (e: IOException) {
                    val msgExcept = Message()
                    msgExcept.obj = e.message
                    puente.sendMessage(msgExcept)
                }
                Looper.loop()
            }
        })
        thread.start()
    }

    private fun recDocAsocFTP(ftpClient: FTPClient, msjRec: String) {
        val fCarpetaImpDocAsoc: String
        var carpetaDocAsoc = prefs.getString("rutacomunicacion", "") ?: ""
        carpetaDocAsoc = if (carpetaDocAsoc == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/docasociados$queBDRoom"
            else "/storage/sdcard0/alba/docasociados"
        } else {
            if (usarMultisistema) "$carpetaDocAsoc/docasociados/$queBDRoom"
            else "$carpetaDocAsoc/docasociados"
        }

        // Nos aseguramos de que la carpeta existe y, si no, la creamos.
        val rutarecepcion = File(carpetaDocAsoc)
        if (!rutarecepcion.exists()) rutarecepcion.mkdirs()
        try {
            // Intentamos cambiar a la carpeta de imagenes.
            fCarpetaImpDocAsoc = if (usarMultisistema) {
                "/" + fConfiguracion.carpetaImportFTP() + "/" + fCodTerminal + dimeRutaEnvSistema() + "/DocAsociados"
            } else {
                "/" + fConfiguracion.carpetaImportFTP() + "/" + fCodTerminal + "/DocAsociados"
            }
            if (ftpClient.changeWorkingDirectory(fCarpetaImpDocAsoc)) {
                // Construimos la lista con los nombres de los ficheros.
                val ftpFiles = ftpClient.listFiles()
                if (ftpFiles != null && ftpFiles.isNotEmpty()) {

                    // Bucle que recorre la lista de ficheros.
                    for (file in ftpFiles) {
                        if (!file.isFile) continue
                        // Controlamos que queremos salir del thread.
                        if (fTerminar) break
                        // Mandamos el mensaje al Handler a traves de un nuevo Message.
                        val msg = Message()
                        msg.obj = msjRec + file.name
                        puente.sendMessage(msg)

                        // Creamos el stream de salida.
                        var output: OutputStream
                        output = FileOutputStream(carpetaDocAsoc + "/" + file.name)
                        // Bajamos el fichero actual de la lista.
                        ftpClient.retrieveFile(file.name, output)
                        // Cerramos el stream de salida.
                        output.close()

                        // Borramos el fichero del servidor.
                        ftpClient.deleteFile(file.name)
                    }
                }
            }
        } catch (e: IOException) {
            val msgExcept = Message()
            msgExcept.obj = e.message
            puente.sendMessage(msgExcept)
        }
    }

    private fun recImagenesFTP(ftpClient: FTPClient, msjRec: String) {
        val fCarpetaImpImag: String
        var carpetaImagenes = prefs.getString("rutacomunicacion", "") ?: ""
        carpetaImagenes = if (carpetaImagenes == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/imagenes/$queBDRoom"
            else "/storage/sdcard0/alba/imagenes"
        } else {
            if (usarMultisistema) "$carpetaImagenes/imagenes/$queBDRoom"
            else "$carpetaImagenes/imagenes"
        }
        try {
            // Intentamos cambiar a la carpeta de imagenes.
            fCarpetaImpImag = if (usarMultisistema) {
                "/" + fConfiguracion.carpetaImportFTP() + "/" + fCodTerminal + dimeRutaEnvSistema() + "/Imagenes"
            } else {
                "/" + fConfiguracion.carpetaImportFTP() + "/" + fCodTerminal + "/Imagenes"
            }
            if (ftpClient.changeWorkingDirectory(fCarpetaImpImag)) {
                // Construimos la lista con los nombres de los ficheros.
                val ftpFiles = ftpClient.listFiles()
                if (ftpFiles != null && ftpFiles.isNotEmpty()) {

                    // Nos aseguramos de que la carpeta existe y, si no, la creamos.
                    // Vaciamos su contenido para no tener imágenes antiguas. Lo hacemos aquí, cuando estamos
                    // seguros de que hay imágenes para recibir.
                    val rutarecepcion = File(carpetaImagenes)
                    if (rutarecepcion.exists()) deleteDirectory(rutarecepcion) else rutarecepcion.mkdirs()

                    // Bucle que recorre la lista de ficheros.
                    for (file in ftpFiles) {
                        if (!file.isFile) continue
                        // Controlamos que queremos salir del thread.
                        if (fTerminar) break
                        // Mandamos el mensaje al Handler a traves de un nuevo Message.
                        val msg = Message()
                        msg.obj = msjRec + file.name
                        puente.sendMessage(msg)

                        // Creamos el stream de salida.
                        var output: OutputStream
                        output = FileOutputStream(carpetaImagenes + "/" + file.name)
                        // Bajamos el fichero actual de la lista.
                        ftpClient.retrieveFile(file.name, output)
                        // Cerramos el stream de salida.
                        output.close()

                        // Borramos el fichero del servidor.
                        ftpClient.deleteFile(file.name)
                    }
                    transformarImagenes(carpetaImagenes)
                }
            }
        } catch (e: IOException) {
            val msgExcept = Message()
            msgExcept.obj = e.message
            puente.sendMessage(msgExcept)
        }
    }

    private fun transformarImagenes(carpetaImagenes: String) {
        val rutaLocalImagenes = File(carpetaImagenes)
        val xmlFiles = rutaLocalImagenes.listFiles()
        if (xmlFiles != null && xmlFiles.isNotEmpty()) {
            for (file in xmlFiles) {
                try {
                    val fin = FileInputStream(file)
                    //val ret = convertStreamToString(fin)      // Esta línea daba error
                    val ret = fin.bufferedReader().use { it.readText() }  // defaults to UTF-8
                    fin.close()
                    val outFile = File(carpetaImagenes + "/" + file.name.replace(".txt", ""))
                    val fos = FileOutputStream(outFile)
                    val bytes = Base64.decodeBase64(ret)
                    fos.write(bytes)
                    fos.close()
                } catch (e: Exception) {
                    System.err.println("Hubo un error de entrada/salida")
                }
            }
        }
    }

    private fun mostrarExcepcion(e: Exception) {
        val msgExcept = Message()
        msgExcept.obj = e.message
        puente.sendMessage(msgExcept)
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Al método salir le envío como parámetro btnRecFTP porque tengo que
            // enviarle algún View.
            salir(btnRecFTP)
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    fun salir(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fImportando) {
            val aldDialog = nuevoAlertBuilder(
                this, resources.getString(R.string.tit_impdatos), resources.getString(
                    R.string.dlg_abandrecep
                ), true
            )
            aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _: DialogInterface?, _: Int -> finish() }
            val alert = aldDialog.create()
            alert.show()
        } else finish()
    }

    private fun dimeRutaEnvSistema(): String {
        return when (queBDRoom) {
            "ibsTablet00.db" -> "/ALBADB00"
            "ibsTablet10.db" -> "/ALBADB10"
            "ibsTablet20.db" -> "/ALBADB20"
            "ibsTablet30.db" -> "/ALBADB30"
            "ibsTablet40.db" -> "/ALBADB40"
            "ibsTablet50.db" -> "/ALBADB50"
            "ibsTablet60.db" -> "/ALBADB60"
            "ibsTablet70.db" -> "/ALBADB70"
            "ibsTablet80.db" -> "/ALBADB80"
            "ibsTablet90.db" -> "/ALBADB90"
            else -> ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == fRequestPedirConfFtp) {
            if (resultCode == RESULT_OK) {
                fServidor = data.getStringExtra("servidorftp") ?: ""
                fUsuario = data.getStringExtra("usuarioftp") ?: ""
                fPassword = data.getStringExtra("passwordftp") ?: ""
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == fRequestPermisoAlmacenamiento) {
            // Si la solicitud es rechazada, el array de resultados (grantResults) está vacía
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                inicializarControles(false)
                Toast.makeText(this, "No tiene permiso de almacenamiento", Toast.LENGTH_LONG).show()
            }
        }
    }


    /*
    @Throws(Exception::class)
    fun convertStreamToString(`is`: InputStream): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        reader.close()
        return sb.toString()
    }
    */


}