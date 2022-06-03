package es.albainformatica.albamobileandroid.actividades

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.comunicaciones.MiscServicio
import org.jetbrains.anko.doAsync
import java.io.File


class ActApkServicio: AppCompatActivity() {
    private var fVersionApk: String = ""
    private lateinit var prefs: SharedPreferences

    private val fRequestPermisoAlmacenamiento = 1

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.actualizar_apk_servicio)

        val intent = intent
        fVersionApk = intent.getStringExtra("versionApk") ?: ""
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            comenzarActualizacion()
        } else run {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), fRequestPermisoAlmacenamiento)
        }
    }



    private fun comenzarActualizacion() {
        val miscServicio = MiscServicio(this)

        doAsync {
            if (miscServicio.descargarApk()) {
                lanzarActualizacion(fVersionApk)
                finish()
            }
        }
    }

    private fun lanzarActualizacion(fVersionApk: String) {
        try {
            var localDirectory = prefs.getString("rutacomunicacion", "") ?: ""
            localDirectory = if (localDirectory == "") Environment.getExternalStorageDirectory().path + "/actualizacion/"
            else "$localDirectory/actualizacion/"

            val file = File("${localDirectory}iTablet21_$fVersionApk.apk")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val fileUri = FileProvider.getUriForFile(baseContext, applicationContext.packageName + ".provider", file)
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
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            fRequestPermisoAlmacenamiento -> {
                // Si la solicitud es rechazada, el array de resultados (grantResults) está vacía
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    comenzarActualizacion()
                } else {
                    Toast.makeText(this, "No tiene permiso de almacenamiento", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }



}