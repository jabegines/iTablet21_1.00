package es.albainformatica.albamobileandroid.comunicaciones

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.sha1
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.net.util.Base64
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


class MiscServicio(context: Context) {
    private val fContext: Context = context
    private var urlServicio: String = ""
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(fContext)
    private var fSistemaId: String = "00"
    private var fCodTerminal: String = ""
    private var fEmail: String = ""
    private var fHuella: String = ""
    private var fPassword: String = ""
    private var aImagArt: ArrayList<Int> = ArrayList()


     init {

        fCodTerminal = prefs.getString("terminal", "") ?: ""
        fEmail = prefs.getString("usuario_servicio", "") ?: ""
        fHuella = Settings.Secure.getString(fContext.contentResolver, Settings.Secure.ANDROID_ID)
        fPassword = prefs.getString("password_servicio", "") ?: ""
        urlServicio = prefs.getString("url_servicio", "") ?: ""

        val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        fSistemaId = if (usarMultisistema) {
            val queBD = queBDRoom
            queBD.substring(queBD.length-2, queBD.length)
        }
        else {
            prefs.getString("sistemaId_servicio", "00") ?: "00"
        }
        fSistemaId = Base64.encodeBase64String(fSistemaId.toByteArray()).replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")
    }


    fun hayPaquetesParaTerminal(): Boolean {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "2"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora

        val queSha = sha1(fFirma)
        fFirma = fFirma + ";;;" + queSha //sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/ListPendingPackages"
        val call = client.newCall(Request.Builder()
                .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId")
                .get()
                .build())

        val response = call.execute()

        return if (response.isSuccessful) {
            // Si hemos recibido Success:true en la respuesta seguimos trabajando
            val queRespuesta = response.body()!!.string()
            if (respuestaCorrecta(queRespuesta)) {
                val fNumPaquete = descomponerNumPaquete(queRespuesta)
                fNumPaquete > 0

            } else false
        } else false
    }


    fun hayComunicacion(): Boolean {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
            val fAccion = "16"
            val fAppId = "1"

            var fFirma =
                fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                        ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
            fFirma = fFirma + ";;;" + sha1(fFirma)
            fFirma = Base64.encodeBase64String(fFirma.toByteArray())
            fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_")
                .replace("=", "*")

            val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
            val queUrl = "$urlServicio/Service/Action/GetVersionApp"
            val call = client.newCall(
                Request.Builder()
                    .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId")
                    .get()
                    .build()
            )

            val response = call.execute()
            return if (response.isSuccessful) {
                val queRespuesta = response.body()?.string() ?: ""
                return (respuestaCorrecta(queRespuesta))

            } else false
            //return response.isSuccessful
        }
        catch (e: Exception) {
            return false
        }
    }



    fun getVersionApk(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "16"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/GetVersionApp"
        val call = client.newCall(Request.Builder()
                .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId")
                .get()
                .build())

        val response = call.execute()

        return if (response.isSuccessful) {
            // Si hemos recibido Success:true en la respuesta seguimos trabajando
            val queRespuesta = response.body()?.string() ?: ""
            if (respuestaCorrecta(queRespuesta)) {
                return descomponerNumVersion(queRespuesta)

            } else ""
        } else ""
    }

    fun descargarApk(): Boolean {
        var localDirectory = prefs.getString("rutacomunicacion", "") ?: ""
        localDirectory = if (localDirectory == "") Environment.getExternalStorageDirectory().path + "/actualizacion/"
        else "$localDirectory/actualizacion/"

        // Nos aseguramos de que la carpeta existe y, si no, la creamos.
        val rutaActualizacion = File(localDirectory)
        if (!rutaActualizacion.exists()) rutaActualizacion.mkdirs()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "15"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/DownloadApp"
        val call = client.newCall(Request.Builder()
                .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId")
                .get()
                .build())

        val response = call.execute()

        return if (response.isSuccessful) {
            if (downloadApk(response, localDirectory)) {
                descomprApk(localDirectory)

            } else false
        }
        else false
    }



    private fun descomprApk(localDirectory: String): Boolean {
        try {
            val fin = FileInputStream("$localDirectory/actualizacion.zip")
            val zin = ZipInputStream(fin)
            var ze: ZipEntry?
            val buffer = ByteArray(1024)
            var continuar = true
            while (continuar) {
                ze = zin.nextEntry
                if (ze != null) {

                    if (ze.name.substring(0, 9)  == "iTablet21") {
                        val fout = FileOutputStream(localDirectory + "/" + ze.name)
                        var c = zin.read(buffer)
                        while (c != -1) {
                            fout.write(buffer, 0, c)
                            c = zin.read(buffer)
                        }

                        zin.closeEntry()
                        fout.close()
                    }
                }
                else continuar = false
            }
            zin.close()
            return true
        }
        catch (e: Exception) {
            return false
        }
    }

    private fun downloadApk(response: Response, localDirectory: String): Boolean {
        val inputStream = response.body()!!.byteStream()
        try {
            val buff = ByteArray(4096)

            val zipFile = File(localDirectory, "actualizacion.zip")
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
        return (File("$localDirectory/actualizacion.zip").exists())
    }


    private fun descomponerNumPaquete(response: String): Int {
        var resultado = 0
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

            resultado = Integer.parseInt(numPaquete)

            queRespuesta = if (posicion > 0) queRespuesta.substring(posicion+3, queRespuesta.length)
            else ""
        }

        return resultado
    }

    private fun descomponerNumVersion(response: String): String {
        var resultado: String
        var posicion = response.indexOf("Cadena1") + 10
        var queRespuesta = response.substring(posicion, response.length)
        posicion = queRespuesta.indexOf('"')
        queRespuesta = queRespuesta.substring(0, posicion)
        resultado = queRespuesta

        posicion = response.indexOf("Cadena2") + 10
        queRespuesta = response.substring(posicion, response.length)
        posicion = queRespuesta.indexOf('"')
        queRespuesta = queRespuesta.substring(0, posicion)

        resultado = "$resultado.$queRespuesta"

        return resultado
    }

    @SuppressLint("SimpleDateFormat")
    fun hayImagenesParaTerminal(): Boolean {

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

            // Tendremos que recorrer aImagArt y ver si tiene algún artículo de los que usemos, porque
            // se puede dar el caso de que haya imágenes nuevas de artículos que no tenemos. Esto funciona así
            // porque el servicio nos devuelve todos los artículos del suscriptor.
            val fArticulos = ArticulosClase(fContext)
            var hayArticulos = false

            for (queArticulo in aImagArt) {
                // El servicio nos devuelve todos los artículos nuevos del suscriptor. Recibiremos sólo los que tengamos en la tablet.
                if (fArticulos.articuloEnTablet(queArticulo)) {
                    hayArticulos = true
                    break
                }
            }

            return hayArticulos
        }
        else return false
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
            if (numArticulo.substring(0, 1) == "B") aImagArt.add(Integer.parseInt(numArticulo.substring(2, numArticulo.length)))
            else aImagArt.add(Integer.parseInt(numArticulo))

            queRespuesta = if (posicion > 0) queRespuesta.substring(posicion+3, queRespuesta.length)
            else ""
        }
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


    private fun respuestaCorrecta(response: String): Boolean {
        val queRespuesta = response.substring(11, 15)
        return queRespuesta == "true"
    }




}