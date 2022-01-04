package es.albainformatica.albamobileandroid.comunicaciones

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.actividades.Main
import es.albainformatica.albamobileandroid.dao.ArticDatAdicDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.sha1
import kotlinx.android.synthetic.main.com_servicio_recibir.*
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.net.util.Base64
import org.jetbrains.anko.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class ServicioRecibir: AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private var fNumPaquete: Int = 0
    private var urlServicio: String = ""
    private var urlServBajarFich: String = ""
    private var urlServBajarImag: String = ""
    private lateinit var fContext: Context
    private var fCodTerminal: String = ""
    private var localDirectory: String = ""
    private var localDirImagenes: String = ""
    private var localDirDocAsoc: String = ""
    private lateinit var handler: Handler
    private var fNumArchivo: Int = 1
    private var fSistemaId: String = "00"
    private var aImagArt: ArrayList<Int> = ArrayList()
    private var aImagBorr: ArrayList<Int> = ArrayList()
    private var fEmail: String = ""
    private var fHuella: String = ""
    private var fPassword: String = ""
    //private var fPuedoRecibir: Boolean = true
    private var fRecibirPaquetes: Boolean = false
    private var fRecibirImag: Boolean = false
    private var fRecibirAutom: Boolean = false
    private var fRecibiendo: Boolean = false

    private lateinit var fArticulos: ArticulosClase
    private lateinit var miscCom: MiscComunicaciones

    private val fRequestPermisoAlmacenamiento = 1


    @SuppressLint("HardwareIds")
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.com_servicio_recibir)
        fContext = this
        fArticulos = ArticulosClase(this)
        miscCom = MiscComunicaciones(fContext, true)

        // Inicializamos las variables
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fCodTerminal = prefs.getString("terminal", "") ?: ""
        fEmail = prefs.getString("usuario_servicio", "") ?: ""
        fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        fPassword = prefs.getString("password_servicio", "") ?: ""
        urlServicio = prefs.getString("url_servicio", "") ?: ""
        urlServBajarFich = "$urlServicio/Service/Action/DownloadPackage"
        urlServBajarImag = "$urlServicio/Service/Action/DownloadProductAttachment"

        val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        fSistemaId = if (usarMultisistema) {
            val queBD = queBDRoom
            queBD.substring(queBD.length - 2, queBD.length)
        }
        else {
            prefs.getString("sistemaId_servicio", "00") ?: "00"
        }
        fSistemaId = Base64.encodeBase64String(fSistemaId.toByteArray()).replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        asignarRutas(usarMultisistema)
        //fPuedoRecibir = Miscelan.puedoRecibir(this)

        val intent = intent
        fRecibirPaquetes = intent.getBooleanExtra("recibirPaquetes", false)
        fRecibirImag = intent.getBooleanExtra("recibirImagenes", false)
        fRecibirAutom = intent.getBooleanExtra("recibirAutom", false)

        inicializarControles()

        // Comprobamos si tenemos el permiso de almacenamiento para, en su caso, pedirlo
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            comenzarRecepcion()
        } else run {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), fRequestPermisoAlmacenamiento)
        }
    }

    override fun onDestroy() {
        fArticulos.close()
        super.onDestroy()
    }


    private fun comenzarRecepcion() {
        if (fRecibirPaquetes) {
            recibir(View(fContext))
        }

        // Podremos recibir imágenes siempre que queramos, independientemente de que tengamos documentos pendientes de enviar o no.
        if (fRecibirImag)
            recImagenes(View(fContext))
    }


    private fun asignarRutas(usarMultisistema: Boolean) {

        localDirectory = prefs.getString("rutacomunicacion", "") ?: ""
        localDirImagenes = localDirectory
        localDirDocAsoc = localDirectory

        localDirectory = if (localDirectory == "") "/storage/sdcard0/alba/recepcion/$fCodTerminal"
        else "$localDirectory/recepcion/$fCodTerminal"

        // Nos aseguramos de que la carpeta de recepción existe y, si no, la creamos.
        val rutarecepcion = File(localDirectory)
        if (!rutarecepcion.exists()) rutarecepcion.mkdirs()

        localDirImagenes = if (localDirImagenes == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/imagenes/" + queBDRoom
            else "/storage/sdcard0/alba/imagenes"
        }
        else {
            if (usarMultisistema) "$localDirImagenes/imagenes/" + queBDRoom
            else "$localDirImagenes/imagenes"
        }
        // Nos aseguramos de que la carpeta de imágenes existe y, si no, la creamos
        val rutaRecepImag = File(localDirImagenes)
        if (!rutaRecepImag.exists()) rutaRecepImag.mkdirs()

        localDirDocAsoc = if (localDirDocAsoc == "") {
            if (usarMultisistema) "/storage/sdcard'/alba/docasociados/" + queBDRoom
            else "/storage/sdcard0/alba/docasociados"
        } else {
            if (usarMultisistema) "$localDirDocAsoc/docasociados/" + queBDRoom
            else "$localDirDocAsoc/docasociados"
        }
        // Nos aseguramos de que la carpeta de documentos asociados existe y, si no, la creamos
        val rutaRecepDocAsoc = File(localDirDocAsoc)
        if (!rutaRecepDocAsoc.exists()) rutaRecepDocAsoc.mkdirs()
    }



    private fun inicializarControles() {

        tvRecibiendo.text = ""
        tvPorcentaje.text = ""
        tvNumArchivos.text = ""
        progressBar.visibility = View.GONE

        handler = @SuppressLint("HandlerLeak")
        object: Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                tvRecibiendo.text = msg.obj.toString()

                if (msg.arg1 > 0) {
                    if (progressBar.max == 0) {
                        progressBar.max = msg.arg1
                        progressBar.visibility = View.VISIBLE
                    }
                    progressBar.incrementProgressBy(1)
                    val sPorcentaje = msg.arg2.toString() + "%"
                    tvPorcentaje.text = sPorcentaje
                    val sNumArchivos = fNumArchivo.toString() + "/" + msg.arg1.toString()
                    tvNumArchivos.text = sNumArchivos
                    fNumArchivo++
                }
            }
        }

        // Si no podemos recibir deshabilitamos el botón para recibir paquetes. Sí podremos recibir imágenes
        // en cualquier momento.
        //if (!fPuedoRecibir) {
        //    btnServRecibir.isEnabled = false

            // Si no hemos entrado desde un botón directo en la pantalla Main, presentamos el mensaje de no poder recibir
        //    if (!fRecibirPaquetes && !fRecibirImag)
        //        MsjAlerta(this).alerta("Tiene documentos, cobros o cargas pendientes de enviar. No podrá recibir.")
        //}
    }



    fun recibir(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        btnServRecibir.isEnabled = false
        btnServRecImag.isEnabled = false

        if (fRecibirAutom) {
            continuarRecepcion()
        } else {
            fContext.alert("¿Comenzar con la recepción?") {
                title = "Recibir"
                yesButton {
                    val msg = Message()
                    msg.obj = "Conectando con el servicio ..."
                    handler.sendMessage(msg)

                    continuarRecepcion()
                }
                noButton { finish() }
            }.show()
        }
    }

    fun recImagenes(view: View) {
        view.isEnabled = false
        fContext.alert("¿Recibir las imágenes?") {
            title = "Recibir imágenes"
            yesButton {
                val msg = Message()
                msg.obj = "Conectando con el servicio ..."
                handler.sendMessage(msg)

                recibirImagenes()
            }
            noButton { finish() }
        }.show()
    }



    private fun recibirImagenes() {
        var fHayImagenes = true
        fRecibiendo = true

        doAsync {
            try {
                var continuar = true

                if (hayImagenesParaTerminal()) {
                    if (aImagArt.count() > 0) continuar = recibirImagenesArt()
                    if (continuar) {
                        if (aImagBorr.count() > 0) continuar = borrarImagenesArt()
                    }
                    if (continuar)
                        confirmarRecogidaImag()

                } else
                    fHayImagenes = false

                uiThread {
                    if (fHayImagenes) {
                        if (continuar) {
                            fContext.alert("Fin de la recepción") {
                                title = "Información"
                                yesButton {
                                    finish()
                                }
                            }.show().setCancelable(false)
                        } else {
                            fContext.alert("Hubo algún problema al recibir") {
                                title = "Información"
                                yesButton {
                                    finish()
                                }
                            }.show().setCancelable(false)
                        }

                    } else {
                        fContext.alert("No se encontraron imágenes nuevas") {
                            title = "Información"
                            yesButton {
                                finish()
                            }
                        }.show().setCancelable(false)
                    }
                }

            } catch (e: Exception) {
                uiThread {
                    Toast.makeText(fContext, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun continuarRecepcion() {
        fRecibiendo = true
        doAsync {

            try {
                hayPaquetesParaTerminal()

                if (fNumPaquete > 0) {
                    var fPaqueteRecibido = false
                    var fPaqueteConfirmado = false

                    if (recibirPaquete()) {
                        fPaqueteRecibido = true
                        fPaqueteConfirmado = confirmarPaquete()
                    }

                    if (fRecibirAutom) {
                        val returnIntent = Intent()
                        setResult(RESULT_OK, returnIntent)
                        finish()

                    } else {
                        uiThread {
                            val fMensaje: String = if (fPaqueteRecibido) {
                                if (fPaqueteConfirmado) "Fin de la importación"
                                else "No se pudo confirmar el paquete"
                            } else {
                                "No se pudo recibir el paquete $fNumPaquete"
                            }

                            // Salimos de la aplicación despues de recibir
                            fContext.alert(fMensaje) {
                                title = "Información"
                                yesButton {
                                    val i = Intent(fContext, Main::class.java)
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ContextCompat.startActivity(fContext, i, null)
                                    finish()
                                }
                            }.show().setCancelable(false)
                        }
                    }
                } else {
                    if (fRecibirAutom) {
                        val returnIntent = Intent()
                        setResult(RESULT_OK, returnIntent)
                        finish()

                    } else {
                        uiThread {
                            fContext.alert("No se encontraron paquetes pendientes de recibir") {
                                title = "Información"
                                yesButton {
                                    finish()
                                }
                            }.show().setCancelable(false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("continuarRecepcion", "continuarRecepcion", e)
            }
        }
    }


    @SuppressLint("HardwareIds", "SimpleDateFormat")
    private fun confirmarPaquete(): Boolean {
        val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "4"
        val fAppId = "1"

        val quePaquete = Base64.encodeBase64String(fNumPaquete.toString().toByteArray()).replace("\r\n", "")
        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(300, TimeUnit.SECONDS).build()
        val urlBuilder = HttpUrl.parse("$urlServicio/Service/Action/ConfirmPackageDownload")?.newBuilder()
        val url = urlBuilder?.build().toString()

        val requestBody = FormBody.Builder().add("Sign", fFirma).add("Package", quePaquete).add("SystemId", fSistemaId).build()
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = client.newCall(request).execute()

        return (response.isSuccessful)
    }


    @SuppressLint("HardwareIds", "SimpleDateFormat")
    private fun recibirImagenesArt(): Boolean {
        val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "6"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        try {

            val b = OkHttpClient.Builder()
            b.readTimeout(5, TimeUnit.MINUTES)
            val client = b.build()

            for (queArticulo in aImagArt) {
                // El servicio nos devuelve todos los artículos del suscriptor. Recibiremos sólo los que tengamos en la tablet.
                if (fArticulos.articuloEnTablet(queArticulo)) {
                    val msg = Message()
                    msg.obj = "Recibiendo imágenes del artículo: $queArticulo"
                    handler.sendMessage(msg)

                    if (downloadImgArt(fFirma, queArticulo, client)) {
                        // Descomprimimos el fichero .zip que nos ha suministrado el servicio. Al descomprimir vamos
                        // colocando en su carpeta las imágenes y los documentos asociados
                        descomprImgArt()
                    }
                }
            }

            return true

        } catch (e: Exception) {
          return false
        }

        //return confirmarRecogidaImag()
    }

    private fun borrarImagenesArt(): Boolean {
        try {
            for (queArticulo in aImagBorr) {
                // El servicio nos devuelve todos los artículos del suscriptor. Recibiremos sólo los que tengamos en la tablet.
                if (fArticulos.articuloEnTablet(queArticulo)) {
                    val msg = Message()
                    msg.obj = "Borrando imágenes del artículo: $queArticulo"
                    handler.sendMessage(msg)

                    //deleteFile("$localDirImagenes/ART_$queArticulo")
                    val fichero = File("$localDirImagenes/ART_$queArticulo.jpg")
                    val borrado = fichero.delete()

                    if (borrado) {
                        // Borramos también los datos adicionales, ya que en el servicio también los hemos borrado.
                        // Por ahora funcionaremos así: si borramos la imagen de un artículo borraremos los datos adicionales.
                        borrarDocAsociados(queArticulo)
                    }
                }
            }
            return true

        } catch (e: Exception) {
            return false
        }
    }


    private fun borrarDocAsociados(queArticulo: Int) {
        // TODO
        /*
        dbAlba.use {
            val articDatAdicDao: ArticDatAdicDao? = MyDatabase.getInstance(this)?.articDatAdicDao()
            val lDatAdic = articDatAdicDao?.getDatosArticulo(queArticulo) ?: emptyList<String>().toMutableList()

            for (datAdic in lDatAdic) {
                val fichero = File(localDirDocAsoc + datAdic)
                fichero.delete()
            }
        }
        */
    }


    @SuppressLint("SimpleDateFormat", "HardwareIds")
    private fun confirmarRecogidaImag(): Boolean {
        val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "10"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(300, TimeUnit.SECONDS).build()
        val urlBuilder = HttpUrl.parse("$urlServicio/Service/Action/ConfirmDownloadAllPendingProductsAttachments")?.newBuilder()
        val url = urlBuilder?.build().toString()

        val requestBody = FormBody.Builder().add("Sign", fFirma).add("SystemId", fSistemaId).build()
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = client.newCall(request).execute()

        return (response.isSuccessful)
    }


    @SuppressLint("HardwareIds", "SimpleDateFormat")
    private fun recibirPaquete(): Boolean {
        val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "3"
        val fAppId = "1"

        val quePaquete = Base64.encodeBase64String(fNumPaquete.toString().toByteArray()).replace("\r\n", "")
        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val msg = Message()
        msg.obj = "Recibiendo paquete con id: $fNumPaquete"
        handler.sendMessage(msg)

        if (downloadFile(fFirma, quePaquete)) {
            // Si conseguimos descomprimir el fichero .zip continuaremos con la importación
            if (descomprimir()) {
                //val miscCom = MiscComunicaciones(fContext, true)
                miscCom.puente = this.handler
                miscCom.xmlABaseDatos()
            }

            return true

        } else {
            val queMensaje = Message()
            queMensaje.obj = "Hubo algún problema y no se pudo descargar el paquete"
            handler.sendMessage(queMensaje)
            return false
        }
    }


    private fun downloadImgArt(fFirma: String, queArticulo: Int, client: OkHttpClient): Boolean {
        try {
            val queArtic64 = Base64.encodeBase64String(queArticulo.toString().toByteArray()).replace("\r\n", "")

            //val b = OkHttpClient.Builder()
            //b.readTimeout(5, TimeUnit.MINUTES)
            //val client = b.build()
            val call = client.newCall(Request.Builder()
                    .url("$urlServBajarImag?Sign=$fFirma&IdProduct=$queArtic64&SystemId=$fSistemaId")
                    .get()
                    .build())

            val response = call.execute()

            if (response.isSuccessful) {

                val inputStream = response.body()!!.byteStream()
                try {
                    val buff = ByteArray(4096)

                    val zipFile = File(localDirectory, "recogida.zip")
                    val output = FileOutputStream(zipFile)
                    while (true) {
                        val readed = inputStream.read(buff)
                        if (readed == -1) {
                            break
                        }
                        output.write(buff, 0, readed)
                    }

                    output.flush()
                    output.close()

                } catch (ignore: IOException) {
                    return false

                } finally {
                    inputStream.close()
                }

                response.close()

                // Devolvemos true si existe el fichero .zip
                return (File("$localDirectory/recogida.zip").exists())

            } else {
                response.close()
                return false
            }

        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun downloadFile(fFirma: String, quePaquete: String): Boolean  {

        val b = OkHttpClient.Builder()
        b.readTimeout(5, TimeUnit.MINUTES)

        val client = b.build()
        val call = client.newCall(Request.Builder()
                .url("$urlServBajarFich?Sign=$fFirma&Package=$quePaquete&SystemId=$fSistemaId")
                .get()
                .build())

        val response = call.execute()

        if (response.isSuccessful) {

            val inputStream = response.body()!!.byteStream()
            try {
                val buff = ByteArray(4096)

                val zipFile = File(localDirectory, "recogida.zip")
                val output = FileOutputStream(zipFile)
                while (true) {
                    val readed = inputStream.read(buff)
                    if (readed == -1) {
                        break
                    }
                    output.write(buff, 0, readed)
                }
                output.flush()
                output.close()

            } catch (ignore: IOException) {
                return false

            } finally {
                inputStream.close()
            }
            // Devolvemos true si existe el fichero .zip
            return (File("$localDirectory/recogida.zip").exists())

        } else return false
    }



    @SuppressLint("SimpleDateFormat")
    private fun hayImagenesParaTerminal(): Boolean {

        aImagArt = ArrayList()
        aImagBorr = ArrayList()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "11"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/ListPendingProductsAttachments"
        val call = client.newCall(Request.Builder()
                .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId")
                .get()
                .build())

        val response = call.execute()

        if (response.isSuccessful) {
            // Si hemos recibido Success:true en la respuesta seguimos trabajando
            val queRespuesta = response.body()!!.string()
            if (respuestaCorrecta(queRespuesta)) {
                if (hayImagenesPend(queRespuesta)) {
                    descomponerArtImagenes(queRespuesta)
                }
            }
        }

        return (aImagArt.count() > 0 || aImagBorr.count() > 0)
    }


    private fun hayImagenesPend(response: String): Boolean {
        var posicion = response.indexOf("ContenidoBase64") + 18
        var queRespuesta = response.substring(posicion, response.length)
        posicion = queRespuesta.indexOf('"')
        queRespuesta = queRespuesta.substring(0, posicion)

        return try {
            val fByteArray = Base64.decodeBase64(queRespuesta)
            queRespuesta = fByteArray.toString(Charset.forName("UTF-8"))

            queRespuesta != ""

        } catch (e: Exception) {
            Log.e("Decompress", "unzip", e)
            false
        }
    }


    @SuppressLint("HardwareIds", "SimpleDateFormat")
    private fun hayPaquetesParaTerminal() {
        val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "2"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                        ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/ListPendingPackages"
        val call = client.newCall(Request.Builder()
                .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId")
                .get()
                .build())

        val msg = Message()
        msg.obj = "Comprobando si hay paquetes ..."
        handler.sendMessage(msg)

        val response = call.execute()

        if (response.isSuccessful) {
            // Si hemos recibido Success:true en la respuesta seguimos trabajando
            val queRespuesta = response.body()!!.string()
            if (respuestaCorrecta(queRespuesta)) {
                descomponerNumPaquete(queRespuesta)
            }

            // si la respuesta no es correcta, presentar mensaje de error

        }
    }


    private fun respuestaCorrecta(response: String): Boolean {
        val queRespuesta = response.substring(11, 15)
        return queRespuesta == "true"
    }


    private fun descomponerArtImagenes(response: String) {
        var numArticulo: String
        var posicion = response.indexOf("ContenidoBase64") + 18
        var queRespuesta = response.substring(posicion, response.length)
        posicion = queRespuesta.indexOf('"')
        queRespuesta = queRespuesta.substring(0, posicion)

        val fByteArray = Base64.decodeBase64(queRespuesta)
        queRespuesta = fByteArray.toString(Charset.forName("UTF-8"))

        while (queRespuesta.isNotEmpty()) {
            posicion = queRespuesta.indexOf(";;;")
            numArticulo = if (posicion > 0) queRespuesta.substring(0, posicion)
            else queRespuesta

            // Los artículos borrados vendrán en el mismo array pero con "B-" por delante
            if (numArticulo.substring(0, 1) == "B") aImagBorr.add(Integer.parseInt(numArticulo.substring(2, numArticulo.length)))
            else aImagArt.add(Integer.parseInt(numArticulo))

            queRespuesta = if (posicion > 0) queRespuesta.substring(posicion + 3, queRespuesta.length)
            else ""
        }
    }


    private fun descomponerNumPaquete(response: String) {
        var numPaquete: String
        var posicion = response.indexOf("ContenidoBase64") + 18
        var queRespuesta = response.substring(posicion, response.length)
        posicion = queRespuesta.indexOf('"')
        queRespuesta = queRespuesta.substring(0, posicion)

        val fByteArray = Base64.decodeBase64(queRespuesta)
        queRespuesta = fByteArray.toString(Charset.forName("UTF-8"))

        while (queRespuesta.isNotEmpty()) {
            posicion = queRespuesta.indexOf(";;;")
            numPaquete = if (posicion > 0) queRespuesta.substring(0, posicion)
            else queRespuesta

            fNumPaquete = Integer.parseInt(numPaquete)

            queRespuesta = if (posicion > 0) queRespuesta.substring(posicion + 3, queRespuesta.length)
            else ""
        }
    }


    private fun descomprImgArt(): Boolean {
        try {
            //val msg = Message()
            //msg.obj = "Descomprimiendo paquete ..."
            //handler.sendMessage(msg)

            val fin = FileInputStream("$localDirectory/recogida.zip")
            val zin = ZipInputStream(fin)
            var ze: ZipEntry?
            val buffer = ByteArray(1024)
            var continuar = true
            while (continuar) {
                ze = zin.nextEntry
                if (ze != null) {

                    val fout: FileOutputStream = if (ze.name.substring(0, 3) == "ART") FileOutputStream(localDirImagenes + "/" + ze.name)
                    else FileOutputStream(localDirDocAsoc + "/" + ze.name)

                    // Vemos si el fichero a descomprimir es una imagen de artículo o un documento asociado,
                    // para descomprimirlo en una carpeta u otra.

                    var c = zin.read(buffer)
                    while (c != -1) {
                        fout.write(buffer, 0, c)
                        c = zin.read(buffer)
                    }
                    zin.closeEntry()
                    fout.close()
                }
                else continuar = false
            }
            zin.close()
            fin.close()
            return true

        } catch (e: Exception) {
            Log.e("Decompress", "unzip", e)
            return false
        }
    }

    private fun descomprimir(): Boolean {
        try {
            val msg = Message()
            msg.obj = "Descomprimiendo paquete ..."
            handler.sendMessage(msg)

            val fin = FileInputStream("$localDirectory/recogida.zip")
            val zin = ZipInputStream(fin)
            var ze: ZipEntry?
            val buffer = ByteArray(1024)
            var continuar = true
            while (continuar) {
                ze = zin.nextEntry
                if (ze != null) {
                    //if (ze.isDirectory) {
                        //_dirChecker(ze.name)
                    //} else {
                        val fout = FileOutputStream(localDirectory + "/" + ze.name)
                        var c = zin.read(buffer)
                        while (c != -1) {
                            fout.write(buffer, 0, c)
                            c = zin.read(buffer)
                        }
                        zin.closeEntry()
                        fout.close()
                    //}
                }
                else continuar = false
            }
            zin.close()
            fin.close()
            return true

        } catch (e: Exception) {
            Log.e("Decompress", "unzip", e)
            return false
        }
    }

    /*
    private fun _dirChecker(dir: String) {
        val f = File(_location + dir)
        if (!f.isDirectory()) {
            f.mkdirs()
        }
    }
    */


/*
    // No podremos recibir siempre que:
    // - tengamos documentos o cobros pendientes de enviar
    // - la fecha del último envío desde el terminal sea mayor que la de la última preparación desde el PC. Esto se comprueba
    // en MiscComunicaciones.xmlABaseDatos()
    private fun puedoRecibir(): Boolean {
        val bd = BaseDatos(this)
        val dbAlba = bd.writableDatabase

        // Puede ocurrir que no tengamos aún la base de datos creada, por eso tenemos que controlar la excepción.
        try {
            var cCursor = dbAlba.rawQuery("SELECT _id FROM cabeceras WHERE estado = 'N' OR estado = 'R'", null)
            cCursor.use {
                return if (it.moveToFirst()) {
                    MsjAlerta(this).alerta("Tiene documentos pendientes de enviar. No podrá recibir.")
                    false
                } else {

                    // Comprobamos que no tenemos cobros realizados
                    cCursor.close()
                    cCursor = dbAlba.rawQuery("SELECT _id FROM cobros", null)
                    val hayCobros = cCursor.moveToFirst()
                    cCursor.close()

                    if (hayCobros) {
                        MsjAlerta(this).alerta("Tiene cobros pendientes de enviar. No podrá recibir.")
                        false
                    } else true
                }
            }
        } catch (e: Exception) {
            return true

        } finally {
            dbAlba.close()
        }
    }
*/


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            fRequestPermisoAlmacenamiento -> {
                // Si la solicitud es rechazada, el array de resultados (grantResults) está vacía
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    comenzarRecepcion()
                } else {
                    Toast.makeText(this, "No tiene permiso de almacenamiento", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }


    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            return if (fRecibiendo) false
            else {
                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()

                // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
                true
            }
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }



}