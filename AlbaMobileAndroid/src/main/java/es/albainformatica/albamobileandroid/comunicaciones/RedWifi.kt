package es.albainformatica.albamobileandroid.comunicaciones

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Message
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import es.albainformatica.albamobileandroid.MsjAlerta
import jcifs.smb.SmbFileOutputStream
import kotlin.Throws
import jcifs.smb.SmbFileInputStream
import android.preference.PreferenceManager
import es.albainformatica.albamobileandroid.R
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception

/**
 * Created by jabegines on 10/10/13.
 */
class RedWifi(private val fContexto: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(fContexto)


    fun puedoRecibirWifi(rutaWifi: String): Boolean {
        val queDominio = prefs.getString("dominio_wifi", "")
        val queUsuario = prefs.getString("usuario_wifi", "")
        val quePassword = prefs.getString("password_wifi", "")
        val auth = NtlmPasswordAuthentication(queDominio, queUsuario, quePassword)
        val path = "smb:$rutaWifi"
        return try {
            val sFile = SmbFile(path + "Rec_ok.xml", auth)
            !sFile.exists()
        } catch (e: Exception) {
            MsjAlerta(fContexto).alerta(e.message ?: "")
            false
        }
    }

    fun enviar(rutaWifi: String, xmlFiles: Array<File>, puente: Handler): Boolean {
        val queDominio = prefs.getString("dominio_wifi", "")
        val queUsuario = prefs.getString("usuario_wifi", "")
        val quePassword = prefs.getString("password_wifi", "")
        val auth = NtlmPasswordAuthentication(queDominio, queUsuario, quePassword)
        val path = "smb:$rutaWifi"
        val msjEnv = fContexto.resources.getString(R.string.msj_EnviandoFich)
        return try {
            // Vemos si existe la carpeta 'Recepcion' para crearla.
            var sFile = SmbFile(path, auth)
            if (!sFile.exists()) sFile.mkdirs()
            for (File in xmlFiles) {
                val fileInputStream = FileInputStream(File)
                sFile = SmbFile(path + File.name, auth)
                val sfos = SmbFileOutputStream(sFile)

                // Mandamos el mensaje al Handler a través de un nuevo Message.
                val msg = Message()
                msg.obj = msjEnv + File.name
                puente.sendMessage(msg)

                // Copiamos en el buffer de salida.
                val buf = ByteArray(16 * 1024 * 1024)
                var len: Int
                while (fileInputStream.read(buf).also { len = it } > 0) {
                    sfos.write(buf, 0, len)
                }
                fileInputStream.close()
                sfos.close()
            }
            true
        } catch (e: Exception) {
            MsjAlerta(fContexto).alerta(e.message ?: "")
            false
        }
    }

    @Throws(Exception::class)
    fun hayImagenes(rutaWifi: String): Boolean {
        val queDominio = prefs.getString("dominio_wifi", "")
        val queUsuario = prefs.getString("usuario_wifi", "")
        val quePassword = prefs.getString("password_wifi", "")
        val auth = NtlmPasswordAuthentication(queDominio, queUsuario, quePassword)
        val path = "smb:$rutaWifi"
        val aFicheros = SmbFile(path, auth).listFiles()
        return try {
            aFicheros.isNotEmpty()
        } catch (e: Exception) {
            MsjAlerta(fContexto).alerta(e.message ?: "")
            false
        }
    }

    @Throws(Exception::class)
    fun recibir(rutaWifi: String, localDirectory: String, puente: Handler): Boolean {
        var resultado: Boolean
        val queDominio = prefs.getString("dominio_wifi", "")
        val queUsuario = prefs.getString("usuario_wifi", "")
        val quePassword = prefs.getString("password_wifi", "")
        val auth = NtlmPasswordAuthentication(queDominio, queUsuario, quePassword)
        val path = "smb:$rutaWifi"
        val aFicheros = SmbFile(path, auth).listFiles()
        val msjRec = fContexto.resources.getString(R.string.msj_RecibiendoFich)
        lateinit var bis: BufferedInputStream
        lateinit var fos: FileOutputStream

        try {
            for (File in aFicheros) {
                bis = BufferedInputStream(SmbFileInputStream(File))
                fos = FileOutputStream(File(localDirectory).toString() + "/" + File.name)

                // Mandamos el mensaje al Handler a través de un nuevo Message.
                val msg = Message()
                msg.obj = msjRec + File.name
                puente.sendMessage(msg)
                val queByte = ByteArray(8192)
                var i: Int
                while (bis.read(queByte).also { i = it } != -1) {
                    fos.write(queByte, 0, i)
                }
                // Hago esto para que no me dé el error en Windows 7 y en algún cliente, cuando recibe mucha cantidad
                // de ficheros: "A device attached to the system is not functioning".
                bis.close()
            }
            // Enviamos el fichero que utilizaremos de bandera para indicar que hemos recogido.
            resultado = copiarFichBandera(path, auth)

        } catch (e: Exception) {
            MsjAlerta(fContexto).alerta(e.message ?: "")
            resultado = false

        } finally {
            try {
                bis.close()
                fos.close()
            } catch (e: Exception) {
                MsjAlerta(fContexto).alerta(e.message ?: "")
                resultado = false
            }
        }
        return resultado
    }

    private fun copiarFichBandera(path: String, auth: NtlmPasswordAuthentication): Boolean {
        return try {
            val sFile = SmbFile(path + "Rec_ok.xml", auth)
            val sfos = SmbFileOutputStream(sFile)
            val buf = ByteArray(1)
            sfos.write(buf)
            sfos.close()
            true
        } catch (e: Exception) {
            MsjAlerta(fContexto).alerta(e.message ?: "")
            false
        }
    }

/*
  public void borrar(String rutaWifi) throws Exception {
    String queDominio = pref.getString("dominio_wifi", "");
    String queUsuario = pref.getString("usuario_wifi", "");
    String quePassword = pref.getString("password_wifi", "");
    NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(queDominio, queUsuario, quePassword);
    String path = "smb:" + rutaWifi;
    try {
      SmbFile[] aFicheros = new SmbFile(path, auth).listFiles();
      for (SmbFile File : aFicheros) {
        try {
          File.delete();
        } catch (SmbException e) {
          Toast.makeText(fContexto, e.getMessage(), Toast.LENGTH_LONG).show();
        }
      }
    } catch (Exception e) {
      Toast.makeText(fContexto, e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }
*/


}