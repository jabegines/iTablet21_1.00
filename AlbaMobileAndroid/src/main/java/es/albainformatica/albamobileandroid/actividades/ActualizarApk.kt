package es.albainformatica.albamobileandroid.actividades

import android.app.Activity
import android.content.Context
import android.widget.TextView
import android.widget.ProgressBar
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.Html
import android.widget.Toast
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.io.CopyStreamListener
import org.apache.commons.net.io.CopyStreamEvent
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.KeyEvent
import es.albainformatica.albamobileandroid.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Created by jabegines on 12/03/14.
 */
class ActualizarApk : Activity() {
    private lateinit var conexionMySQL: Connection
    private lateinit var puente: Handler
    private lateinit var otroPuente: Handler

    private lateinit var tvDatos: TextView
    private lateinit var tvTotalBytes: TextView
    private lateinit var tvBytes: TextView
    private lateinit var pbLarge: ProgressBar
    private lateinit var tvPorc: TextView

    private var progressBarStatus = 0
    private lateinit var fConfiguracion: Configuracion
    private lateinit var pref: SharedPreferences
    private var nombreApk: String = ""
    private var versionApk: String = ""
    private var tamanyoApk: Long = 0
    private var fDescargando: Boolean = false


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.actualizar_apk)

        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
        conectar()
    }

    private fun inicializarControles() {
        fDescargando = false
        tvDatos = findViewById(R.id.txtResultadoSQL)
        tvTotalBytes = findViewById(R.id.tvTotalBytes)
        tvBytes = findViewById(R.id.tvBytes)
        tvPorc = findViewById(R.id.tvPorc)
        pbLarge = findViewById(R.id.pbLarge)
        progressBarStatus = 0

        // Leemos las preferencias de la aplicación;
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        puente = object : Handler() {
            override fun handleMessage(msg: Message) {
                tvDatos.text = tvDatos.text.toString() + Html.fromHtml("<br />") + msg.obj.toString()
            }
        }
        otroPuente = object : Handler() {
            override fun handleMessage(msg: Message) {
                tvTotalBytes.text = "Total bytes a descargar: $tamanyoApk"
                tvBytes.text = msg.obj.toString()
                tvPorc.text = "$progressBarStatus%"
            }
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.mni_actualizar)
    }

    private fun conectar() {
        val thread = Thread {
            Looper.prepare()
            try {
                val codProducto = fConfiguracion.codigoProducto()
                var puedoActualizar = false
                var existeCodProducto = false
                var msgResultado = Message()
                msgResultado.obj = "Conectando con Alba Informática ..."
                puente.sendMessage(msgResultado)
                // Conectamos con la base de datos y consultamos el código de producto y almacén 1000, que nos indicará
                // que es un programa Android.
                val sqlEjecutar = "SELECT alta, version FROM clientes WHERE codProducto = '$codProducto' AND almacen = 1000"
                conectarBDMySQL("u_battlement", "p_battlement", "albainformatica.es", "3306", "albainfo_battlement")
                val st = conexionMySQL.createStatement()
                val rs = st.executeQuery(sqlEjecutar)

                // Comprobamos si el cliente tiene la marca de poder actualizar.
                if (rs.first()) {
                    msgResultado = Message()
                    msgResultado.obj = "Solicitando permisos ..."
                    puente.sendMessage(msgResultado)
                    existeCodProducto = true
                    val actualizar = rs.getObject(1).toString().toInt()
                    if (actualizar == 1) {
                        puedoActualizar = true
                        versionApk = rs.getObject(2).toString()
                    }
                } else {
                    msgResultado = Message()
                    msgResultado.obj = "No se encontró el código de producto"
                    puente.sendMessage(msgResultado)
                }
                rs.close()
                st.close()
                if (puedoActualizar) {
                    msgResultado = Message()
                    msgResultado.obj = "Comenzando descarga ..."
                    puente.sendMessage(msgResultado)
                    descargarApk()
                    lanzarActualizacion()
                } else {
                    if (existeCodProducto) {
                        msgResultado = Message()
                        msgResultado.obj = "No se obtuvo permiso para actualizar"
                        puente.sendMessage(msgResultado)
                    }
                }
                // A continuación insertamos un registro en la tabla HistoricoDescargas para registrar el proceso de actualización.
                insertarEnHcoDescargas(codProducto, puedoActualizar)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error: " + e.message, Toast.LENGTH_SHORT).show()
            }
            Looper.loop()
        }
        thread.start()
    }

    private fun insertarEnHcoDescargas(codProducto: String, puedoActualizar: Boolean) {
        try {
            val correcto: String = if (puedoActualizar) "1" else "0"
            val sqlEjecutar =
                "INSERT INTO historicodescargas (codProducto, almacen, version, fecha, correcto)" +
                        " VALUES ('" + codProducto + "', 1000, '" + versionApk + "', CURRENT_TIMESTAMP, " + correcto + ")"
            val st = conexionMySQL.createStatement()
            st.executeUpdate(sqlEjecutar)
            st.close()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun conectarBDMySQL(usuario: String, contrasena: String, ip: String, puerto: String, catalogo: String) {
        //if (conexionMySQL == null) {
            val urlConexionMySQL: String =
                if (catalogo != "") "jdbc:mysql://$ip:$puerto/$catalogo"
                else "jdbc:mysql://$ip:$puerto"

            if ((usuario != "") and (contrasena != "") and (ip != "") and (puerto != "")) {
                try {
                    Class.forName("com.mysql.jdbc.Driver")
                    conexionMySQL =
                        DriverManager.getConnection(urlConexionMySQL, usuario, contrasena)
                } catch (e: ClassNotFoundException) {
                    Toast.makeText(applicationContext, "Error: " + e.message, Toast.LENGTH_SHORT)
                        .show()
                } catch (e: SQLException) {
                    Toast.makeText(applicationContext, "Error: " + e.message, Toast.LENGTH_LONG)
                        .show()
                    tvDatos.text = e.message
                }
            }
        //}
    }

    private fun descargarApk() {
        try {
            fDescargando = true
            val ftpClient = FTPClient()
            ftpClient.connect("ftp.albainformatica.es")
            val login = ftpClient.login("descargas", "dscrgs07")
            val nombreDescarga: String
            if (login) {
                // Establecemos el tamaño del buffer. Si no hacemos esto, la descarga irá muy lenta.
                ftpClient.bufferSize = 1024 * 1024
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
                ftpClient.copyStreamListener = createListener()

                // Vemos la carpeta de comunicación que tenemos en preferencias.
                val queRuta = pref.getString("rutacomunicacion", "")
                val localDirectory: String = if (queRuta == "") "/storage/sdcard0/alba/actualizacion" else "$queRuta/actualizacion"
                nombreApk = "$localDirectory/AlbaMobileAndroid_$versionApk.apk"
                nombreDescarga = "AlbaMobileAndroid_$versionApk.apk"

                // Nos aseguramos de que la carpeta existe y, si no, la creamos.
                val rutarecepcion = File(localDirectory)
                if (!rutarecepcion.exists()) rutarecepcion.mkdirs()

                // Entramos en modo pasivo.
                ftpClient.enterLocalPassiveMode()
                // Creamos el stream de salida.
                val output: OutputStream = FileOutputStream(nombreApk)
                // Obtenemos el tamaño del fichero, para mostrar porcentajes de descarga.
                val file = ftpClient.mlistFile(nombreDescarga)
                tamanyoApk = file.size

                // Bajamos el fichero actual de la lista.
                ftpClient.retrieveFile(nombreDescarga, output)
                // Cerramos el stream de salida.
                output.close()
                ftpClient.noop()
                ftpClient.logout()
                ftpClient.disconnect()
                fDescargando = false
            }
        } catch (e: IOException) {
            val msgExcept = Message()
            msgExcept.obj = e.message
            puente.sendMessage(msgExcept)
        }
    }

    private fun createListener(): CopyStreamListener {
        return object : CopyStreamListener {
            override fun bytesTransferred(event: CopyStreamEvent) {
                bytesTransferred(
                    event.totalBytesTransferred,
                    event.bytesTransferred,
                    event.streamSize
                )
            }

            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                val msgExcept = Message()
                msgExcept.obj = "Bytes transferidos: $totalBytesTransferred"
                otroPuente.sendMessage(msgExcept)

                // Actualizamos el ProgressBar
                val l = totalBytesTransferred * 100 / tamanyoApk
                progressBarStatus = l.toInt()
                pbLarge.progress = progressBarStatus
            }
        }
    }

    private fun lanzarActualizacion() {
        try {
            val file = File(nombreApk)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val fileUri = FileProvider.getUriForFile(
                    baseContext,
                    applicationContext.packageName + ".provider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW, fileUri)
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Al método salir le envío como parámetro btnRecFTP porque tengo que
            // enviarle algún View.
            salir()
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    fun salir() {
        if (fDescargando) {
            val aldDialog = NuevoAlertBuilder(
                this, resources.getString(R.string.tit_impdatos),
                resources.getString(R.string.dlg_abandrecep), true
            )
            aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _, _ -> finish() }
            val alert = aldDialog.create()
            alert.show()
            ColorDividerAlert(this, alert)
        } else finish()
    }
}